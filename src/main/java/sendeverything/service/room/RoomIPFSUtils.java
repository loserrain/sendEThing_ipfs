package sendeverything.service.room;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.sql.exec.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import sendeverything.models.User;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.DBRoomFileChunk;
import sendeverything.models.room.Room;
import sendeverything.repository.DBRoomFileChunkRepository;
import sendeverything.repository.DBRoomFileRepository;
import sendeverything.repository.RoomRepository;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @Author: luowenxing
 * @Date: 2021/5/31 21:30
 */
@Component
public class RoomIPFSUtils {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private DBRoomFileChunkRepository dbRoomFileChunkRepository;
    @Autowired
    private DBRoomFileRepository dbRoomFileRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
//    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 根据需要调整线程池大小

    public RoomIPFSUtils( RoomRepository roomRepository,DBRoomFileChunkRepository dbRoomFileChunkRepository,  DBRoomFileRepository dbRoomFileRepository) {
        this.roomRepository = roomRepository;
        this.dbRoomFileChunkRepository = dbRoomFileChunkRepository;
        this.dbRoomFileRepository = dbRoomFileRepository;
    }

    /**
     * ipfs的服务器地址和端口,替换成自己的ip，port
     */
    private final IPFS IPFS = new IPFS("/ip4/127.0.0.1/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/140.130.33.153/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/169.254.183.111/tcp/5001");



    public DBRoomFile findByFileId(String fileId) {
        return dbRoomFileRepository.findByFileId(fileId).orElse(null);
    }





    public  synchronized DBRoomFile storeFile(String fileId, String outputFileName, Optional<User> user,Long filesize,String description,String roomCode,String uploaderName) {
        // 这里不需要再次检查文件是否存在，因为这已在调用此方法之前检查过
        Room room=roomRepository.findByRoomCode(roomCode);
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime newTime = createTime.plusHours(8);

        DBRoomFile dbFile = new DBRoomFile(outputFileName, fileId, newTime);
        dbFile.setFileId(fileId); // 设置 ID
        dbFile.setVerificationCode(generateUniqueVerificationCode());
        dbFile.setFileSize(filesize);
        dbFile.setRoom(room);
        dbFile.setUploaderName(uploaderName);
        dbFile.setDescription(description);
        user.ifPresent(dbFile::setUser) ;



        dbRoomFileRepository.save(dbFile);
        return dbFile;
    }


    public  void uploadPart(int chunkNumber, DBRoomFile dbFile, String chunkId, MultipartFile fileChunk, int totalChunks) throws IOException {
        String cid= upload(fileChunk.getBytes());
        DBRoomFileChunk fileChunkEntity = new DBRoomFileChunk(chunkId, dbFile, totalChunks, chunkNumber,cid);
        // 创建上传分片请求
        System.out.println(chunkNumber + " uploaded successfully");
        dbRoomFileChunkRepository.save(fileChunkEntity);
        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
    }
    public  String upload(byte[] data) throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(data);
        MerkleNode addResult = IPFS.add(file).get(0);
        return addResult.hash.toString();
    }

    public  Optional<DBRoomFile> getFileByFileId(String fileId) {
        return dbRoomFileRepository.findByFileId(fileId);
    }



    public  byte[] download(String hash) {
        byte[] data = null;
        try {
            data = IPFS.cat(Multihash.fromBase58(hash));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public  void download(String hash, String destFile) {
        byte[] data = null;
        try {
            data = IPFS.cat(Multihash.fromBase58(hash));
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data != null && data.length > 0) {
            File file = new File(destFile);
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
    public byte[] downloadChunk(String cid) throws IOException {
        // 假设 IPFS 已经在构造函数中被初始化
        Multihash filePointer = Multihash.fromBase58(cid);
        return IPFS.cat(filePointer);
    }
    public void writeToResponseStreamConcurrently3(DBRoomFile dbFile, HttpServletResponse response) throws IOException {
        List<DBRoomFileChunk> chunks = dbRoomFileChunkRepository.findByDbRoomFileOrderByChunkNumberAsc(dbFile);
        int totalChunks = chunks.size();
        int batchSize = 5;  // 假设每批次处理5个分片
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        int chunksDownloaded = 0;

        try {
            for (int i = 0; i < chunks.size(); i += batchSize) {
                List<DBRoomFileChunk> batch = chunks.subList(i, Math.min(chunks.size(), i + batchSize));
                List<Future<byte[]>> futures = new ArrayList<>();

                for (DBRoomFileChunk chunk : batch) {
                    Callable<byte[]> task = () -> {
                        byte[] data = downloadChunk(chunk.getCid());
                        System.out.println("Chunk " + chunk.getChunkNumber() + " downloaded.");
                        return data;
                    };
                    futures.add(executorService.submit(task));
                }

                for (Future<byte[]> future : futures) {
                    try {
                        byte[] data = future.get();
                        response.getOutputStream().write(data);
                        chunksDownloaded++;
                        double progress = (double) chunksDownloaded / totalChunks * 100;
                        // 发送进度信息
                        messagingTemplate.convertAndSend("/topic/downloadProgress", progress);

                        Arrays.fill(data, (byte) 0); // 清理敏感数据
                    } catch (ExecutionException | InterruptedException e) {
                        System.err.println("Error processing chunk: " + e.getMessage());
                        e.printStackTrace();
                    } catch (java.util.concurrent.ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } finally {
            executorService.shutdown();
        }
    }


    private  String generateUniqueVerificationCode() {
        Random random = new Random();
        String verificationCode;
        do {
            int code = random.nextInt(900000) + 100000;
            verificationCode = String.valueOf(code);
        } while (isCodeExists(verificationCode));

        return verificationCode;
    }
    private  boolean isCodeExists(String code) {
        return dbRoomFileRepository.existsById(Long.valueOf(code));
    }

    public  DBRoomFile findByVerificationCode(String verificationCode) {
        return dbRoomFileRepository.findByVerificationCode(verificationCode).orElse(null);
    }

    public void unpinAndCollectGarbage(DBRoomFile dbFile) throws IOException {
        for (DBRoomFileChunk chunk : dbFile.getDbRoomFileChunks()) {
            String cid = chunk.getCid();
            IPFS.pin.rm(Multihash.fromBase58(cid));
            System.out.println("Unpinned CID: " + cid);
        }


        dbRoomFileRepository.delete(dbFile);
        System.out.println("Garbage collection executed.");
    }
}




