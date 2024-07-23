package sendeverything.service;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hibernate.Hibernate;
import org.hibernate.sql.exec.ExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import sendeverything.models.User;
import sendeverything.repository.DatabaseFileRepository;
import sendeverything.repository.FileChunkProjection;
import sendeverything.repository.FileChunkRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;


/**
 * @Author: luowenxing
 * @Date: 2021/5/31 21:30
 */
@Component
public class IPFSUtils {



    @Autowired
    private  DatabaseFileRepository dbFileRepository;
    @Autowired
    private  FileChunkRepository fileChunkRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;


//    private final IPFS IPFS = new IPFS("/ip4/140.130.33.152/tcp/5001");


//    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // 根据需要调整线程池大小




    public IPFSUtils(DatabaseFileRepository dbFileRepository,FileChunkRepository fileChunkRepository) {
        this.dbFileRepository = dbFileRepository;
        this.fileChunkRepository = fileChunkRepository;
    }
    /**
     * ipfs的服务器地址和端口,替换成自己的ip，port
     */

    private final String clusterApiUrl = "http://your-cluster-ip:9094";
    private final IPFS IPFS = new IPFS("/ip4/127.0.0.1/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/140.130.33.153/tcp/5001");
//    private final IPFS IPFS = new IPFS("/ip4/169.254.183.111/tcp/5001");



    public DatabaseFile findByFileId(String fileId) {
        return dbFileRepository.findByFileId(fileId).orElse(null);
    }







    public  synchronized DatabaseFile storeFile(String fileId, String outputFileName, Optional<User> user,Long filesize){
        // 这里不需要再次检查文件是否存在，因为这已在调用此方法之前检查过
        LocalDateTime createTime = LocalDateTime.now();
        DatabaseFile dbFile = new DatabaseFile(outputFileName, fileId, createTime);
        dbFile.setFileId(fileId); // 设置 ID
        dbFile.setVerificationCode(generateUniqueVerificationCode());
        dbFile.setFileSize(filesize);
        user.ifPresent(dbFile::setUser);
        dbFileRepository.save(dbFile);
        return dbFile;
    }


    public  void uploadPart(int chunkNumber, DatabaseFile dbFile, String chunkId, MultipartFile fileChunk, int totalChunks) throws IOException {
        String cid= upload(fileChunk.getBytes());
        FileChunk fileChunkEntity = new FileChunk(chunkId, dbFile, totalChunks, chunkNumber,cid);
        // 创建上传分片请求
        System.out.println(chunkNumber + " uploaded successfully");
        fileChunkRepository.save(fileChunkEntity);
        // 注意：你可能需要存储每个分片的ETag值，以便后续完成上传时使用
    }
    public  String upload(byte[] data) throws IOException {
        NamedStreamable.ByteArrayWrapper file = new NamedStreamable.ByteArrayWrapper(data);
        MerkleNode addResult = IPFS.add(file).get(0);
        return addResult.hash.toString();
    }

    public  Optional<DatabaseFile> getFileByFileId(String fileId) {
        return dbFileRepository.findByFileId(fileId);
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


    public byte[] downloadChunk(String cid) throws IOException {
        // 假设 IPFS 已经在构造函数中被初始化
        Multihash filePointer = Multihash.fromBase58(cid);
        return IPFS.cat(filePointer);
    }



    public void updateDownloadProgress(String uuid, double progress) {

        messagingTemplate.convertAndSend("/topic/downloadProgress/" + uuid, progress);

    }



    public void writeToResponseStreamConcurrently3(DatabaseFile dbFile, HttpServletResponse response, String uuid) throws IOException {
        List<FileChunk> chunks = fileChunkRepository.findByDatabaseFileOrderByChunkNumberAsc(dbFile);
        int totalChunks = chunks.size();
        int batchSize = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        int chunksDownloaded = 0;

        try {
            for (int i = 0; i < chunks.size(); i += batchSize) {
                List<FileChunk> batch = chunks.subList(i, Math.min(chunks.size(), i + batchSize));
                List<Future<byte[]>> futures = new ArrayList<>();

                for (FileChunk chunk : batch) {
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
                        updateDownloadProgress(uuid, progress);
                        Arrays.fill(data, (byte) 0);
                    } catch (ExecutionException | InterruptedException | java.util.concurrent.ExecutionException e) {
                        System.err.println("Error processing chunk: " + e.getMessage());
                        if (e.getCause() instanceof IOException) {
                            throw (IOException) e.getCause(); // 抛出IOException来处理客户端断开连接的情况
                        }
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client aborted connection: " + e.getMessage());
            // 处理客户端中断连接的情况，例如可以选择不再写入响应
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ex) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
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
        return dbFileRepository.existsById(code);
    }

    public  DatabaseFile findByVerificationCode(String verificationCode) {
        return dbFileRepository.findByVerificationCode(verificationCode).orElse(null);
    }
    public void unpinAndCollectGarbage(DatabaseFile dbFile) throws IOException {
        for (FileChunk chunk : dbFile.getFileChunks()) {
            String cid = chunk.getCid();
            System.out.println(cid);
            IPFS.pin.rm(Multihash.fromBase58(cid));
            System.out.println("Unpinned CID: " + cid);
        }


        dbFileRepository.delete(dbFile);
        System.out.println("Garbage collection executed.");
    }

    @Scheduled(cron = "0 0 * * * *")  // 每小時執行一次
    @Transactional
    public void cleanupOldFiles() throws IOException {
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2).plusHours(8);
        List<DatabaseFile> oldFiles = dbFileRepository.findAllOlderThanTwoDays(twoDaysAgo);
        for (DatabaseFile file : oldFiles) {
                System.out.println("Cleaning up old file: " + file.getFileChunks());
                unpinAndCollectGarbage(file);
            }
        }









}





