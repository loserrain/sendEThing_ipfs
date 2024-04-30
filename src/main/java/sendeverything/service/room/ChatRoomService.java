package sendeverything.service.room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import sendeverything.models.ChatRoomMessage;
import sendeverything.models.User;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.Room;
import sendeverything.models.room.RoomType;
import sendeverything.payload.request.ChatMessage;
import sendeverything.payload.response.BigChatRoomResponse;
import sendeverything.payload.response.ChatRoomFileResponse;
import sendeverything.payload.response.FileNameResponse;
import sendeverything.payload.response.RoomResponse;
import sendeverything.repository.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ChatRoomService {
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;
    private final RoomRepository roomRepository;
    private final DBRoomFileRepository dbRoomFileRepository;

    private final ChatRoomMessageRepository chatRoomMessageRepository;
    @Autowired
    public ChatRoomService(ChatRoomMessageRepository chatRoomMessageRepository, UserRepository userRepository, UserRoomRepository userRoomRepository, RoomRepository roomRepository,DBRoomFileRepository dbRoomFileRepository) {
          this.chatRoomMessageRepository = chatRoomMessageRepository;
            this.userRepository = userRepository;
            this.userRoomRepository = userRoomRepository;
            this.roomRepository = roomRepository;
            this.dbRoomFileRepository = dbRoomFileRepository;
    }

    public List<ChatRoomMessage> getMessagesBefore(String roomCode, LocalDateTime lastTimestamp,int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        return chatRoomMessageRepository.findMessagesBeforeDate(roomCode, lastTimestamp, pageable);
        }
    public ChatRoomMessage saveMessage(ChatMessage chatMessage) {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("now"+ now);

        ChatRoomMessage chatRoomMessage = new ChatRoomMessage();
        chatRoomMessage.setContent(chatMessage.getContent());
        chatRoomMessage.setRoomCode(chatMessage.getRoomCode());
        chatRoomMessage.setSender(chatMessage.getSender());
        chatRoomMessage.setTimestamp(now);
        chatRoomMessage.setType(chatMessage.getType());

        chatRoomMessageRepository.save(chatRoomMessage);
        return chatRoomMessage;
    }
    public List<String>  getRoomsByUser(String username) {
        User user=userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Error: User not found."));
        return userRoomRepository.findRoomCodesByUser(user);
    }

//    public List<String>  getRoomsByUser(String username) {
//        User user=userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Error: User not found."));
//        return userRoomRepository.findRoomCodesByUser(user);
//    }





    public RoomResponse getRoomInfo(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode);
        if (room == null) {
            // 如果找不到房间，则返回 null 或抛出异常，视情况而定
            return null;
        }

        RoomResponse roomResponse = new RoomResponse();
        roomResponse.setRoomCode(room.getRoomCode());
        roomResponse.setTitle(room.getTitle());
        roomResponse.setDescription(room.getDescription());
        roomResponse.setRoomType(room.getRoomType());
        // 设置其他房间信息

        // 如果房间图像是 Blob 类型，可以转换为 Base64 字符串
        String imageBase64 = blobToBase64String(room.getImage());
        roomResponse.setImage(imageBase64);

        return roomResponse;
    }

    private String blobToBase64String(Blob blob) {
        if (blob == null) {
            return null;
        }
        try {
            InputStream inputStream = blob.getBinaryStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] blobBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(blobBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert BLOB to string", e);
        }
    }




    public BigChatRoomResponse getBigChatRoomResponse(String userRoom, ChatRoomMessage chatRoomMessage, RoomResponse chatRoomInfo) {
        return  BigChatRoomResponse.builder()
                .chatRoomTitle(chatRoomInfo.getTitle())
                .chatRoomImage(chatRoomInfo.getImage())
                .chatRoomMessage(chatRoomMessage.getContent())
                .chatRoomTime(chatRoomMessage.getTimestamp().toString())
                .chatRoomCode(userRoom)
                .chatRoomDescription(chatRoomInfo.getDescription())
                .chatRoomType(chatRoomInfo.getRoomType().toString())
                .build();
    }

    public ChatRoomFileResponse chatRoomFileResponses(String roomCode) {
         Room room = roomRepository.findByRoomCode(roomCode);
         List<DBRoomFile>dbRoomFileList = dbRoomFileRepository.findAllByRoom(room);
         List<FileNameResponse> fileNameResponses = new ArrayList<>();
            for (DBRoomFile dbRoomFile : dbRoomFileList) {
                FileNameResponse fileNameResponse = new FileNameResponse();
                fileNameResponse.setFileName(dbRoomFile.getFileName());
                fileNameResponse.setContent(dbRoomFile.getDescription());
                fileNameResponse.setVerificationCode(dbRoomFile.getVerificationCode());
                fileNameResponse.setFileSize(dbRoomFile.getFileSize());
                fileNameResponse.setCreatedAt(dbRoomFile.getTimestamp());
                fileNameResponses.add(fileNameResponse);
            }


        return new ChatRoomFileResponse(room.getBoardType(),fileNameResponses);
    }

    public boolean isSecretRoom(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode);
        return room.getRoomType().equals(RoomType.SECRET);
    }

}
