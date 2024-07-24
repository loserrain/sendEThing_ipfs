package sendeverything.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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
import sendeverything.models.room.Room;
import sendeverything.payload.request.ChatMessage;
import sendeverything.payload.request.CodeRequest;
import sendeverything.payload.request.RoomCodeRequest;
import sendeverything.payload.request.RoomTypeRequest;
import sendeverything.payload.response.*;
import sendeverything.repository.ChatRoomMessageRepository;
import sendeverything.repository.RoomRepository;
import sendeverything.repository.UserRepository;
import sendeverything.security.services.AuthenticationService;
import sendeverything.service.room.BulletinService;
import sendeverything.service.room.ChatRoomService;

import java.io.IOException;
import java.security.Principal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//@CrossOrigin(origins = "http://localhost:8080, http://localhost:8081, http://localhost:8080", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Controller
public class ChatController {
    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private BulletinService bulletinService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private ChatRoomMessageRepository chatRoomMessageRepository;

    //使用者發送訊息，並根據使用者名稱取得使用者頭像
    @MessageMapping("/chat.sendMessage/{roomCode}")
    @SendTo("/topic/{roomCode}")
    public ChatImageDTO sendMessage(

            @Payload ChatMessage chatMessage
    ) throws SQLException, IOException {
        ChatRoomMessage chatRoomMessage = chatRoomService.saveMessage(chatMessage);
        String senderImage = authenticationService.getProfileImageBase64(chatMessage.getSender());  // 假设这是获取用户头像的方法
//        String senderImage = authenticationService.getImgUrl(chatMessage.getSender());
        System.out.println("senderImage: " + chatMessage.getSender() + " " + chatMessage.getContent());
        IsJoinedResponse isJoinedResponse = new IsJoinedResponse(true, true);
        return new ChatImageDTO(chatRoomMessage, senderImage, isJoinedResponse);


    }

    //使用者加入聊天室，並判斷是否已經加入過
    @MessageMapping("/join.joinMessage/{roomCode}")
    @SendTo("/topic/{roomCode}")
    public ChatImageDTO joinMessage(

            @Payload ChatMessage chatMessage
    ) throws SQLException, IOException {
        String username = chatMessage.getSender();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Error: User not found."));
        Room room = roomRepository.findByRoomCode(chatMessage.getRoomCode());
        boolean isAlreadyJoined = bulletinService.isAlreadyJoined(user, room);
        System.out.println("isAlreadyJoined: " + isAlreadyJoined);
        if (isAlreadyJoined) {
            System.out.println("Already joined");
            return new ChatImageDTO(new IsJoinedResponse(true, false));
        } else {
            System.out.println("New joined");

            return new ChatImageDTO(new IsJoinedResponse(false, false));
        }
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

    //取得聊天室訊息
    @PostMapping("/getMessages")
    public List<ChatImageDTO> getMessagesBefore(@RequestBody RoomCodeRequest roomCodeRequest, Principal principal) throws SQLException, IOException {
            String roomCode = roomCodeRequest.getRoomCode();

            String test2 = principal.getName();
            LocalDateTime chatRoomTimestamp = chatRoomMessageRepository.findUserBySenderAndRoomCode(test2, roomCodeRequest.getRoomCode()).get(0).getTimestamp();
//            System.out.println("chatRoomTimestamp: " + chatRoomTimestamp);

            LocalDateTime lastTimestamp = roomCodeRequest.getLastTimestamp();
//            System.out.println("lastTimestamp: " + lastTimestamp);
//            取得兩個時間內的訊息
            List<ChatRoomMessage> testMessages=chatRoomService.getMessagesDateToDate(roomCodeRequest.getRoomCode(), lastTimestamp, chatRoomTimestamp,20);




            List<ChatImageDTO> chatImageDTOS = new ArrayList<ChatImageDTO>();

            for (ChatRoomMessage chatRoomMessage : testMessages) {
                String username= chatRoomMessage.getSender();
                String senderImage= authenticationService.getProfileImageBase64(username);
//                String senderImage= authenticationService.getImgUrl(username);

                chatImageDTOS.add(new ChatImageDTO(chatRoomMessage,  senderImage));
            }
            return chatImageDTOS;

    }



    @PostMapping("/getNewMessages")
    public List<ChatImageDTO> getMessagesNew(
            @RequestBody RoomCodeRequest roomCode,Principal principal
            ) throws SQLException, IOException {

            String test2 = principal.getName();
            LocalDateTime chatRoomTimestamp = chatRoomMessageRepository.findUserBySenderAndRoomCode(test2, roomCode.getRoomCode()).get(0).getTimestamp();
            LocalDateTime lastTimestamp = LocalDateTime.now();
            String roomChatCode = roomCode.getRoomCode();
//            System.out.println(roomChatCode);
            List<ChatRoomMessage> testMessages=chatRoomService.getMessagesDateToDate(roomCode.getRoomCode(), lastTimestamp, chatRoomTimestamp,20);
//
            List<ChatRoomMessage> chatRoomMessages=chatRoomService.getMessagesBefore(roomChatCode, lastTimestamp,20);
            List<ChatImageDTO> chatImageDTOS = new ArrayList<>();
            for (ChatRoomMessage chatRoomMessage : testMessages) {
                String username= chatRoomMessage.getSender();
                String senderImage= authenticationService.getProfileImageBase64(username);
//                String senderImage= authenticationService.getImgUrl(username);
                chatImageDTOS.add(new ChatImageDTO(chatRoomMessage, senderImage));
//                System.out.println("chatRoomMessage: " + chatRoomMessage);
            }

//        System.out.println(chatImageDTOS);
            return chatImageDTOS;
    }

    @PostMapping("/getNewChatMessages")
    public List<ChatImageDTO> getNewChatMessages(
            @RequestBody RoomCodeRequest roomCode,Principal principal
    ) throws SQLException, IOException {

        String test2 = principal.getName();
        LocalDateTime chatRoomTimestamp = chatRoomMessageRepository.findUserBySenderAndRoomCode(test2, roomCode.getRoomCode()).get(0).getTimestamp();
//        System.out.println("chatRoomTimestamp: " + chatRoomTimestamp);

        LocalDateTime lastTimestamp = LocalDateTime.now();
        String roomChatCode = roomCode.getRoomCode();
//        System.out.println(roomChatCode);
        List<ChatRoomMessage> testMessages=chatRoomService.getMessagesDateToDate(roomCode.getRoomCode(), lastTimestamp, chatRoomTimestamp,20);
        List<ChatRoomMessage> chatRoomMessages=chatRoomService.getMessagesBefore(roomChatCode, lastTimestamp,20);
        List<ChatImageDTO> chatImageDTOS = new ArrayList<>();
        for (ChatRoomMessage chatRoomMessage : testMessages) {
            String senderImage= "";
            chatImageDTOS.add(new ChatImageDTO(chatRoomMessage, senderImage));
//            System.out.println("chatRoomMessage: " + chatRoomMessage);
        }
        return chatImageDTOS;
    }


    @GetMapping("/getMessageByUser")
    public ResponseEntity<?> getMessagesNew(Principal principal){
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }
//        System.out.println("fkdlgjklfdgjklsfdjgklgfd: "+ principal.getName());
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


        System.out.println("userRoomsuserRoomsuserRoomsuserRoomsuserRooms;: "+userRooms);
        LocalDateTime lastTimestamp = LocalDateTime.now();
        List<BigChatRoomResponse> bigChatRoomResponses = new ArrayList<>();
        for(String userRoom : userRooms){
            if(chatRoomService.isSecretRoom(userRoom)){
                List<ChatRoomMessage> chatRoomMessagesResponse= chatRoomService.getMessagesBefore(userRoom, lastTimestamp,1);
                System.out.println("userRoom: "+userRoom);
                System.out.println("userName: "+username);
                System.out.println("chatRoomMessageRepository.findUserBySenderAndRoomCode(username, userRoom): "+chatRoomMessageRepository.findUserBySenderAndRoomCode(username, userRoom));
//                LocalDateTime chatRoomTimestamp = chatRoomMessageRepository.findUserBySenderAndRoomCode(username, userRoom).get(0).getTimestamp();
//                List<ChatRoomMessage> testMessages=chatRoomService.getMessagesDateToDate(userRoom, lastTimestamp, chatRoomTimestamp,1);
//                System.out.println("testMessages: "+testMessages);
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

    @PostMapping("/getSharedKeysByUser")
    public ResponseEntity<?> getSharedKeysByUser(@RequestBody RoomTypeRequest request, Principal principal){
        String roomType = request.getRoomType().toString();
        System.out.println("roomType: " + roomType);
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
        }

        String username = principal.getName();
        List<ChatKeyAndVectorResponse> sharedKeys = chatRoomService.getSharedKeysByUser(username, roomType);
        return ResponseEntity.ok(sharedKeys);
    }






}