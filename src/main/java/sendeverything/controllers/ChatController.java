package sendeverything.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sendeverything.models.ChatRoomMessage;
import sendeverything.models.User;
import sendeverything.models.room.UserRoom;
import sendeverything.payload.request.ChatMessage;
import sendeverything.payload.request.CodeRequest;
import sendeverything.payload.request.RoomCodeRequest;
import sendeverything.payload.response.BigChatRoomResponse;
import sendeverything.payload.response.ChatImageDTO;
import sendeverything.payload.response.ChatRoomFileResponse;
import sendeverything.payload.response.RoomResponse;
import sendeverything.security.services.AuthenticationService;
import sendeverything.service.room.ChatRoomService;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "http://localhost:8080, http://localhost:8081, http://localhost:8080", maxAge = 3600, allowCredentials="true")

@RestController
@RequestMapping("/api/auth")
@Controller
public class ChatController {
    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private AuthenticationService authenticationService;


    @MessageMapping("/chat.sendMessage/{roomCode}")
    @SendTo("/topic/{roomCode}")
    public ChatImageDTO sendMessage(

            @Payload ChatMessage chatMessage
    ) throws SQLException, IOException {

        ChatRoomMessage chatRoomMessage = chatRoomService.saveMessage(chatMessage);
        String senderImage = authenticationService.getProfileImageBase64(chatMessage.getSender());  // 假设这是获取用户头像的方法

        return new ChatImageDTO(chatRoomMessage, senderImage);


    }





    @MessageMapping("/chat.addUser")
    @SendTo("/topic/{roomCode}")
    public ChatMessage addUser(
            @PathVariable String roomCode,
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Add username in web socket session
//        System.out.println(chatMessage);
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());

        return chatMessage;
    }
    @PostMapping("/getMessages")
    public List<ChatImageDTO> getMessagesBefore(@RequestBody RoomCodeRequest roomCodeRequest) throws SQLException, IOException {
            String roomCode = roomCodeRequest.getRoomCode();
            LocalDateTime lastTimestamp = roomCodeRequest.getLastTimestamp();
            System.out.println(lastTimestamp);
            List<ChatRoomMessage> chatRoomMessages = chatRoomService.getMessagesBefore(roomCode, lastTimestamp,20);
            List<ChatImageDTO> chatImageDTOS = new ArrayList<ChatImageDTO>();
            for (ChatRoomMessage chatRoomMessage : chatRoomMessages) {
                String username= chatRoomMessage.getSender();
                String senderImage= authenticationService.getProfileImageBase64(username);

                chatImageDTOS.add(new ChatImageDTO(chatRoomMessage, senderImage));
            }
            return chatImageDTOS;

    }



    @PostMapping("/getNewMessages")
    public List<ChatImageDTO> getMessagesNew(
            @RequestBody RoomCodeRequest roomCode,Principal principal
            ) throws SQLException, IOException {


            LocalDateTime lastTimestamp = LocalDateTime.now();
            String roomChatCode = roomCode.getRoomCode();
//            System.out.println(roomChatCode);
            List<ChatRoomMessage> chatRoomMessages=chatRoomService.getMessagesBefore(roomChatCode, lastTimestamp,20);
            List<ChatImageDTO> chatImageDTOS = new ArrayList<>();
            for (ChatRoomMessage chatRoomMessage : chatRoomMessages) {
                String username= chatRoomMessage.getSender();
                String senderImage= authenticationService.getProfileImageBase64(username);

                chatImageDTOS.add(new ChatImageDTO(chatRoomMessage, senderImage));
            }
//        System.out.println(chatImageDTOS);
            return chatImageDTOS;
    }

    @PostMapping("/getNewChatMessages")
    public List<ChatImageDTO> getNewChatMessages(
            @RequestBody RoomCodeRequest roomCode,Principal principal
    ) throws SQLException, IOException {


        LocalDateTime lastTimestamp = LocalDateTime.now();
        String roomChatCode = roomCode.getRoomCode();
        List<ChatRoomMessage> chatRoomMessages=chatRoomService.getMessagesBefore(roomChatCode, lastTimestamp,20);
        List<ChatImageDTO> chatImageDTOS = new ArrayList<>();
        for (ChatRoomMessage chatRoomMessage : chatRoomMessages) {
            String senderImage= "";
            chatImageDTOS.add(new ChatImageDTO(chatRoomMessage, senderImage));
        }
        return chatImageDTOS;
    }


    @GetMapping("/getMessageByUser")
    public ResponseEntity<?> getMessagesNew(Principal principal){
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }
        String username = principal.getName();
        List<String> userRooms= chatRoomService.getRoomsByUser(username);
        LocalDateTime lastTimestamp = LocalDateTime.now();
        List<BigChatRoomResponse> bigChatRoomResponses = new ArrayList<>();
        for(String userRoom : userRooms){
            if(!(chatRoomService.isSecretRoom(userRoom))) {
                List<ChatRoomMessage> chatRoomMessagesResponse = chatRoomService.getMessagesBefore(userRoom, lastTimestamp, 1);
                RoomResponse chatRoomInfo = chatRoomService.getRoomInfo(userRoom);
                for (ChatRoomMessage chatRoomMessage : chatRoomMessagesResponse) {
                    BigChatRoomResponse bigChatRoomResponse = chatRoomService.getBigChatRoomResponse(userRoom, chatRoomMessage, chatRoomInfo);
                    bigChatRoomResponses.add(bigChatRoomResponse);
                }
            }
        }
        return ResponseEntity.ok(bigChatRoomResponses);
    }


    @GetMapping("/getSecretMessageByUser")
    public ResponseEntity<?> getChatMessageByUser(Principal principal){
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }
        String username = principal.getName();
        List<String> userRooms= chatRoomService.getRoomsByUser(username);
        LocalDateTime lastTimestamp = LocalDateTime.now();
        List<BigChatRoomResponse> bigChatRoomResponses = new ArrayList<>();
        for(String userRoom : userRooms){
            if(chatRoomService.isSecretRoom(userRoom)){
            List<ChatRoomMessage> chatRoomMessagesResponse= chatRoomService.getMessagesBefore(userRoom, lastTimestamp,1);
            RoomResponse chatRoomInfo = chatRoomService.getRoomInfo(userRoom);
            for (ChatRoomMessage chatRoomMessage : chatRoomMessagesResponse) {
                BigChatRoomResponse bigChatRoomResponse = chatRoomService.getBigChatRoomResponse(userRoom, chatRoomMessage, chatRoomInfo);
                bigChatRoomResponses.add(bigChatRoomResponse);
            }}
        }
        return ResponseEntity.ok(bigChatRoomResponses);
    }
    @PostMapping("/getFileInfoByRoomCode")
    public ResponseEntity<?> getFileInfoByRoomCode(@RequestBody CodeRequest codeRequest){
        String roomCode = codeRequest.getCode();
        System.out.println(roomCode);
        ChatRoomFileResponse chatRoomFileResponses = chatRoomService.chatRoomFileResponses(roomCode);
        return ResponseEntity.ok(chatRoomFileResponses);
    }





}