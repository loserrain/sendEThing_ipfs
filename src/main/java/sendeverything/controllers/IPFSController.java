package sendeverything.controllers;

import com.google.zxing.WriterException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import sendeverything.models.User;
import sendeverything.payload.request.SpeedTestRequest;
import sendeverything.payload.response.FileNameResponse;
import sendeverything.payload.response.FileResponse;
import sendeverything.repository.DatabaseFileRepository;
import sendeverything.repository.FileChunkRepository;
import sendeverything.repository.UserRepository;

import sendeverything.service.IPFSUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
//@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class IPFSController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FileChunkRepository fileChunkRepository;

    @Autowired
    private DatabaseFileRepository dbFileRepository;
    @Autowired
    private IPFSUtils IPFSUtils;

    //取得已登入使用者上傳檔案列表
    @GetMapping("getFiles")
    public ResponseEntity<?> getFiles(Principal principal) {
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok().body("User is not logged in!");
        }
        List<DatabaseFile> dbFiles = dbFileRepository.findAllByUserOrderByTimestampDesc(optionalUser.orElse(null));
        LocalDateTime now = LocalDateTime.now();


        List<FileNameResponse> fileNameResponses = dbFiles.stream()
                .map(file -> {
                    LocalDateTime createTimeTwoDaysLater = file.getTimestamp().plusDays(2);
                    Duration remainingDuration = Duration.between(now, createTimeTwoDaysLater);
                    long remainingDays = remainingDuration.toDays(); // 可以選擇以天數來表示
                    long remainingHours = remainingDuration.toHours() % 24; // 或者以小時數來表示
                    String remainingTimeFormatted = remainingDays + "D " + remainingHours + "H";  // 1 D 23:59
                    return new FileNameResponse(
                            file.getFileName(),
                            file.getVerificationCode(),
                            file.getFileSize(),
                            file.getTimestamp(),
                            remainingTimeFormatted // or remainingDuration in any other unit you prefer
                    );
                })
                .toList();
        return ResponseEntity.ok().body(fileNameResponses);
    }

    //上傳分片至ipfs
    @PostMapping("/uploadChunk")
    public ResponseEntity<?> uploadChunk(@RequestParam("fileChunk") MultipartFile fileChunk,
                                         @RequestParam("chunkNumber") int chunkNumber,
                                         @RequestParam("totalChunks") int totalChunks,
                                         @RequestParam("fileId") String fileId,
                                         @RequestParam("chunkId") String chunkId,
                                         @RequestParam("size") Long fileSize,
                                         @RequestParam("outputFileName") String outputFileName,
                                         Principal principal) throws IOException {
        System.out.println("Principal: " + principal);
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();

        // 檔案存在性檢查
        DatabaseFile dbFile = dbFileRepository.findByFileId(fileId).orElse(null);

        if (dbFile == null) {
            synchronized (IPFSUtils.class) {
                // 再次检查确保没有其他线程已经创建了文件
                dbFile = dbFileRepository.findByFileId(fileId).orElse(null);
                if (dbFile == null) {
                    dbFile = IPFSUtils.storeFile(fileId, outputFileName, optionalUser, fileSize);
                }
            }
        }

        System.out.println("Uploading chunk " + chunkNumber + " of file " + fileId);
        //分片存在性檢查，實施斷點續傳，藉以達到秒傳效果
        FileChunk dbfileChunk = fileChunkRepository.findByChunkIdAndDatabaseFile_FileId(chunkId, fileId).orElse(null);
        if (dbfileChunk == null) {
            IPFSUtils.uploadPart(chunkNumber, dbFile, chunkId, fileChunk, totalChunks);

            return ResponseEntity.ok("Chunk " + chunkNumber + " uploaded successfully");
        } else {
            System.out.println("Chunk " + chunkNumber + " already uploaded");
            return ResponseEntity.ok("Chunk " + chunkNumber + " already uploaded");
        }
    }
    //完成上傳
    @PostMapping("/completeUpload")
    public FileResponse completeUpload(
            @RequestParam("outputFileName") String outputFileName,
            @RequestParam("fileId") String fileId
    ) throws IOException, SQLException, WriterException {
        Optional<DatabaseFile> dbFileOptional = IPFSUtils.getFileByFileId(fileId);
        // 确保找到了文件
        if (dbFileOptional.isEmpty()) {
            // 文件未找到，处理错误情况，例如抛出异常或返回错误响应
            throw new FileNotFoundException("File not found with fileId: " + fileId);
        }

        DatabaseFile dbFile = dbFileOptional.get();
        User user = dbFile.getUser();
        String username;

        if (user != null && user.getUsername() != null) {
            username = user.getUsername();
        } else {
            username = "Anonymous";
        }


        System.out.println(username + "completeUpload : " + dbFile.getFileName() + " VerificationCode : " + dbFile.getVerificationCode());


        return new FileResponse(dbFile.getVerificationCode(), "");
    }

    //透過驗證碼至ipfs取得檔案，透過uuid 使用websocket監控下載進度

    @GetMapping("/downloadFileByCode/{verificationCode}/{uuid}")
    public ResponseEntity<?> downloadFile(@PathVariable String verificationCode,
                                          @PathVariable String uuid, HttpServletResponse response) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }

            String encodedFileName = URLEncoder.encode(dbFile.getFileName(), StandardCharsets.UTF_8);
            System.out.println("dbFile: " + dbFile.getFileChunks());
//
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(dbFile.getFileSize()));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);


            IPFSUtils.writeToResponseStreamConcurrently3(dbFile, response, uuid);
            System.out.println(response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }

    //刪除檔案
    @GetMapping("/cleanupByCode/{verificationCode}")
    public ResponseEntity<?> cleanupResources(@PathVariable String verificationCode) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }

            System.out.println("Cleaning up resources for dbFile: " + dbFile.getFileChunks());
            IPFSUtils.unpinAndCollectGarbage(dbFile);

            return ResponseEntity.ok().body("Cleanup successful for file with verification code: " + verificationCode);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getFileNameByCode/{verificationCode}")
    public ResponseEntity<?> getFileNameByCode(@PathVariable String verificationCode) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }
            FileResponse fileNameResponse = new FileResponse(dbFile.getFileName());

            return ResponseEntity.ok().body(fileNameResponse);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    //刪除資料庫檔案
    @GetMapping("/deleteFileByCode/{verificationCode}")
    public ResponseEntity<?> deleteFileByCode(@PathVariable String verificationCode) {
        try {
            DatabaseFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }
            dbFileRepository.delete(dbFile);

            return ResponseEntity.ok().body("Cleanup successful for file with verification code: " + verificationCode);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}













