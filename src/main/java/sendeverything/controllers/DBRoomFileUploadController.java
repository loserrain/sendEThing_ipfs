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
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.DBRoomFileChunk;
import sendeverything.payload.response.FileResponse;
import sendeverything.repository.*;
import sendeverything.service.CodeGenerator;
import sendeverything.service.IPFSUtils;
import sendeverything.service.room.RoomIPFSUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

//@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class DBRoomFileUploadController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DBRoomFileChunkRepository dbRoomFileChunkRepository;
    @Autowired
    private CodeGenerator codeGenerator;
    @Autowired
    private DBRoomFileRepository dbRoomFileRepository;
    @Autowired
    private RoomIPFSUtils IPFSUtils;


    @PostMapping("/uploadRoomFileChunk")
    public ResponseEntity<?> uploadChunk(@RequestParam("fileChunk") MultipartFile fileChunk,
                                         @RequestParam("chunkNumber") int chunkNumber,
                                         @RequestParam("totalChunks") int totalChunks,
                                         @RequestParam("fileId") String fileId,
                                         @RequestParam("chunkId") String chunkId,
                                         @RequestParam("size") Long fileSize,
                                         @RequestParam("outputFileName") String outputFileName,
                                         @RequestParam("description") String description,
                                         @RequestParam("roomCode") String RoomCode,
                                         @RequestParam("uploaderName") String uploaderName,
                                         Principal principal) throws IOException {
        System.out.println("Principal: " + principal);
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
        Optional<DBRoomFile> optionalDbFile = dbRoomFileRepository.findByFileIdAndRoom_RoomCode(fileId, RoomCode);


        // 先尝试查找文件，避免不必要的同步操作
        DBRoomFile dbFile = optionalDbFile.orElse(null);
        if (dbFile == null) {
            synchronized (IPFSUtils.class) {
                // 再次检查确保没有其他线程已经创建了文件
                dbFile = dbRoomFileRepository.findByFileId(fileId).orElse(null);
                if (dbFile == null) {
                    dbFile = IPFSUtils.storeFile(fileId, outputFileName, optionalUser,fileSize,description,RoomCode,uploaderName);
                }
            }
        }
        DBRoomFileChunk dbRoomFileChunk = dbRoomFileChunkRepository.findByChunkIdAndDbRoomFile_FileIdAndDbRoomFile_Room_RoomCode(chunkId, fileId, RoomCode).orElse(null);

        if (dbRoomFileChunk == null) {
            IPFSUtils.uploadPart(chunkNumber, dbFile, chunkId, fileChunk, totalChunks);

            return ResponseEntity.ok("Chunk " + chunkNumber + " uploaded successfully");
        } else {
            System.out.println("Chunk " + chunkNumber + " already uploaded");
            return ResponseEntity.ok("Chunk " + chunkNumber + " already uploaded");
        }
    }

    @PostMapping("/completeUploadRoomFile")
    public FileResponse completeUpload(
            @RequestParam("outputFileName") String outputFileName,
            @RequestParam("fileId") String fileId
    ) throws IOException, SQLException, WriterException {
        Optional<DBRoomFile> dbFileOptional = IPFSUtils.getFileByFileId(fileId);
        // 确保找到了文件
        if (dbFileOptional.isEmpty()) {
            // 文件未找到，处理错误情况，例如抛出异常或返回错误响应
            throw new FileNotFoundException("File not found with fileId: " + fileId);
        }

        DBRoomFile dbFile = dbFileOptional.get();
        List<DBRoomFileChunk> sortedChunks = dbFile.getDbRoomFileChunks().stream()
                .sorted(Comparator.comparingInt(DBRoomFileChunk::getChunkNumber))
                .toList();

        System.out.println("Sorted dbFile chunks: " + sortedChunks);



        return new FileResponse(dbFile.getVerificationCode(), "");
    }



    @GetMapping("/downloadRoomFileByCode/{verificationCode}")
    public ResponseEntity<?> downloadFile(@PathVariable String verificationCode, HttpServletResponse response) {
        try {
            DBRoomFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
            if (dbFile == null) {
                return ResponseEntity.notFound().build();
            }
            String encodedFileName = URLEncoder.encode(dbFile.getFileName(), StandardCharsets.UTF_8);
            System.out.println("dbFile: " + dbFile.getDbRoomFileChunks());
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(dbFile.getFileSize()));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
            response.setHeader(HttpHeaders.CONTENT_TYPE,"application/octet-stream");

            IPFSUtils.writeToResponseStreamConcurrently3(dbFile, response);
            System.out.println(response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }}


        @GetMapping("/cleanupRoomFileByCode/{verificationCode}")
        public ResponseEntity<?> cleanupResources(@PathVariable String verificationCode) {
            try {
                DBRoomFile dbFile = IPFSUtils.findByVerificationCode(verificationCode);
                if (dbFile == null) {
                    return ResponseEntity.notFound().build();
                }

                System.out.println("Cleaning up resources for dbFile: " + dbFile.getDbRoomFileChunks());
                IPFSUtils.unpinAndCollectGarbage(dbFile);

                return ResponseEntity.ok().body("Cleanup successful for file with verification code: " + verificationCode);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

