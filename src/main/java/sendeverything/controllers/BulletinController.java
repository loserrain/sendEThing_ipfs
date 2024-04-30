package sendeverything.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.models.User;
import sendeverything.models.room.*;
import sendeverything.payload.request.BoardRequest;
import sendeverything.payload.request.RoomRequest;
import sendeverything.payload.response.RoomCodeResponse;
import sendeverything.payload.response.RoomContentResponse;
import sendeverything.payload.response.RoomResponse;
import sendeverything.repository.UserRepository;
import sendeverything.security.services.AuthenticationService;
import sendeverything.service.room.BulletinService;
import sendeverything.service.room.ChatRoomService;
import software.amazon.awssdk.services.s3.model.MultipartUpload;

import java.security.Principal;
import java.sql.Blob;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class BulletinController {
    @Autowired
    private  BulletinService bulletinService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChatRoomService chatRoomService;
    @Autowired
    private AuthenticationService authenticationService;



    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom(@RequestParam String title,
                                       @RequestParam RoomType roomType,
                                       @RequestParam String roomDescription,
                                       @RequestParam String roomPassword,
                                       @RequestParam MultipartFile roomImage,
                                       @RequestParam BoardType boardType,
                                       Principal principal) throws Exception {
        System.out.println("title: "+boardType);
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();

        System.out.println("optionalUser: "+optionalUser);

        if (!(roomType.equals(RoomType.SECRET))) {
            String roomCode = bulletinService.saveRoom(title, roomDescription, roomPassword, roomImage, optionalUser, roomType, boardType);

            return ResponseEntity.ok(new RoomCodeResponse(roomCode)) ;
        }else{
            assert principal != null;
            String username= principal.getName();
            Blob image= authenticationService.convertBase64ToBlob(authenticationService.getProfileImageBase64(username));
            String roomCode = bulletinService.saveSecretRoom(title, roomDescription, roomPassword, image, optionalUser, roomType, boardType);


            Room room = bulletinService.findByRoomCode1(roomCode);
            if(bulletinService.isAlreadyJoined(optionalUser.orElse(null),room)){
                return ResponseEntity.ok("Create Room Success: "+roomCode);
            }
            bulletinService.joinRoom(optionalUser.orElse(null),room );

            return ResponseEntity.ok(new RoomCodeResponse(roomCode)) ;

        }



    }
    @PostMapping("/getAllRooms")
    public ResponseEntity<List<RoomResponse>> getAllRooms(Principal principal,@RequestBody BoardRequest boardRequest) {
        BoardType boardType = boardRequest.getBoardType();
//        String username1 = principal.getName();
//        List<UserRoom>userRooms= chatRoomService.getRoomsByUser(username1);
//        for (UserRoom userRoom : userRooms) {
//            System.out.println("user"+userRoom.getRoom().getRoomCode());
//
//        }

        List<RoomResponse> roomResponses = bulletinService.getRoomsByType(principal,boardType);
        System.out.println("roomResponses: "+roomResponses);
        return ResponseEntity.ok(roomResponses);
    }

    @PostMapping("/accessRoom")
    public ResponseEntity<?> accessRoom(@RequestBody RoomRequest roomRequest, HttpServletResponse response,Principal principal){
        String roomCode = roomRequest.getRoomCode();
        String password = roomRequest.getPassword();
        String roomType = roomRequest.getRoomType();

        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
        Room room= bulletinService.findByRoomCode1(roomCode);
        RoomResponse roomResponse = bulletinService.accessRoom(roomCode, password);
        System.out.println("roomResponse: "+roomRequest);
        if(roomType.equals("PUBLIC")){
            if(bulletinService.isAlreadyJoined(optionalUser.orElse(null),room)){
                return ResponseEntity.ok("Access Room Success: "+roomCode);
            }
            bulletinService.joinRoom(optionalUser.orElse(null),room );
            return ResponseEntity.ok("Access Room Success: "+roomCode);
        }
        if (roomResponse != null ) {
            // 登入成功，設置 cookie
            String roomCookie= bulletinService.hashRoomCode(roomCode);
            Cookie cookie = new Cookie(roomCode, roomCookie);
            cookie.setHttpOnly(true); // 使 cookie 為 HTTP Only，提高安全性
            cookie.setPath("/"); // 設置 cookie 的路徑，如果需要限制為特定路徑，可以進行調整

            cookie.setMaxAge(60 * 60 ); // 設置 cookie 的有效期，例如這裡是一個小時
            response.addCookie(cookie);
            if(bulletinService.isAlreadyJoined(optionalUser.orElse(null),room)){
                return ResponseEntity.ok("Access Room Success: "+roomCode);
            }
            bulletinService.joinRoom(optionalUser.orElse(null),room );
            System.out.println("cookie: "+cookie.getValue());
            return ResponseEntity.ok("Access Room Success: "+roomCode);
        } else {
            // 登入失敗處理，例如返回一個錯誤響應
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect room code or password");
        }}
    @PostMapping("/verifyCookie")
    public ResponseEntity<String> checkCookie(HttpServletRequest request,@RequestBody RoomRequest RoomRequest) {
        String roomCode = RoomRequest.getRoomCode();

        String roomType =bulletinService.findByRoomCode1(roomCode).getRoomType().toString();
        System.out.println("roomCode: "+roomCode);
        Cookie[] cookies = request.getCookies();
        if(roomType.equals("PUBLIC")){
            return ResponseEntity.ok("Authentication passed");
        }

        if (cookies != null && roomCode != null && !roomCode.isEmpty()) {
            for (Cookie cookie : cookies) {
                // 检查cookie名称是否与roomcode相匹配
                if (roomCode.equals(cookie.getName())) {
                    System.out.println("cookie: " + cookie.getValue());

                    return ResponseEntity.ok("Authentication passed");
                }
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: No valid cookie found");
    }

//    @PostMapping("/showRoomContent")
//    public ResponseEntity<?> showRoomContent(@RequestBody RoomRequest RoomRequest) {
//        String roomCode = RoomRequest.getRoomCode();
//
//        RoomResponse roomResponse = bulletinService.findByRoomCode(roomCode);
//
//        return ResponseEntity.ok(roomResponse);
//    }

    @PostMapping("/showRoomContent")
    public ResponseEntity<?> showRoomContent(@RequestBody RoomRequest roomRequest,Principal principal){
        String roomCode = roomRequest.getRoomCode();
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();

        Room room = bulletinService.findByRoomCode1(roomCode);
        RoomResponse roomResponse = bulletinService.findByRoomCode(roomCode);
        List<DBRoomFile> dbRoomFiles = room.getDbRoomFiles();
        List<DBRoomDTO> dtos = dbRoomFiles.stream()
                .map(file -> new DBRoomDTO(file.getFileSize(),file.getFileName(),file.getDescription(),file.getTimestamp(),file.getVerificationCode(),file.getUploaderName()))
                .collect(Collectors.toList());

        RoomContentResponse contentResponse = new RoomContentResponse();
        if(optionalUser.isPresent() && room.getOwner() != null){
            if(room.getOwner().equals(optionalUser.get())){
                contentResponse.setIsRoomOwner(true);
            }else {contentResponse.setIsRoomOwner(false);}

        }


        contentResponse.setRoomResponse(roomResponse);
        contentResponse.setDbRoomFiles(dtos);


        return ResponseEntity.ok(contentResponse);
    }

//    @GetMapping("/getCreatedRoom")
//    public RoomResponse getCreatedRoom(Principal principal) {
//        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
//        List<Room> rooms = bulletinService.getCreatedRooms(optionalUser.orElse(null));
//
//
//    }



}