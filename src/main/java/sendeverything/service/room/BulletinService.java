package sendeverything.service.room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.exception.RoomNotFoundException;
import sendeverything.models.User;
import sendeverything.models.room.BoardType;
import sendeverything.models.room.Room;
import sendeverything.models.room.RoomType;
import sendeverything.models.room.UserRoom;
import sendeverything.payload.response.RoomResponse;
import sendeverything.repository.RoomRepository;
import sendeverything.repository.UserRepository;
import sendeverything.repository.UserRoomRepository;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BulletinService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;

    @Autowired
    public BulletinService(RoomRepository roomRepository, UserRepository userRepository, UserRoomRepository userRoomRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.userRoomRepository = userRoomRepository;
    }
    private String generateRoomCode() {
        Random random = new Random();
        String verificationCode;
        do {
            char[] vowels = {'a', 'e', 'i', 'o', 'u'};
            char[] consonants = {'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v'};

            StringBuilder codeBuilder = new StringBuilder(6);
            for (int i = 0; i < 8; i++) {
                // Alternate between consonants and vowels
                if (i % 2 == 0) { // Even index: consonant
                    codeBuilder.append(consonants[random.nextInt(consonants.length)]);
                } else { // Odd index: vowel
                    codeBuilder.append(vowels[random.nextInt(vowels.length)]);
                }
            }
            verificationCode = codeBuilder.toString().toUpperCase(Locale.ROOT);
            System.out.println("Verification code: " + verificationCode);
        } while (isCodeExists(verificationCode));

        return verificationCode;
    }
    private  boolean isCodeExists(String code) {
        return roomRepository.existsByRoomCode(code);
    }

    public String saveRoom(String title, String roomDescription , String roomPassword , MultipartFile roomImage , Optional<User> user, RoomType roomType, BoardType boardType) throws Exception {
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime newTime = createTime.plusHours(8);
        Blob image = convertToBlob(roomImage);

        Room room = new Room(generateRoomCode(),title,roomDescription,roomPassword,image,roomType,boardType,newTime);
        user.ifPresent(room::setOwner);
        roomRepository.save(room);
        return room.getRoomCode();
    }



    public String saveSecretRoom(String title, String roomDescription , String roomPassword , Blob roomImage , Optional<User> user, RoomType roomType, BoardType boardType) throws Exception {
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime newTime = createTime.plusHours(8);



        Room room = new Room(generateRoomCode(),title,roomDescription,roomPassword,roomImage,roomType,boardType,newTime);
        user.ifPresent(room::setOwner);
        roomRepository.save(room);
        return room.getRoomCode();
    }

    public List<RoomResponse> getRoomsByType(Principal principal, BoardType boardType) {
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
        User currentUser = optionalUser.orElse(null);

        List<Room> rooms = roomRepository.findByBoardType(boardType); // 假设这个方法已经在RoomRepository中定义
        return rooms.stream()
                .map(room -> convertToRoomResponse(room, currentUser))
                .collect(Collectors.toList());
    }

    private RoomResponse convertToRoomResponse(Room room, User currentUser) {
        RoomResponse roomResponse = new RoomResponse(); // 假设您已有一个构造RoomResponse的方法
        roomResponse.setRoomCode(room.getRoomCode());
        roomResponse.setTitle(room.getTitle());
        roomResponse.setDescription(room.getDescription());
        roomResponse.setRoomType(room.getRoomType());
        roomResponse.setCreateTime(room.getCreateTime());
        // 设置RoomResponse的其他属性...

        boolean isOwner = room.getOwner() != null && room.getOwner().equals(currentUser);
        roomResponse.setIsOwner(isOwner);
        boolean isMember = userRoomRepository.existsByUserAndRoom(currentUser, room);
        roomResponse.setIsMember(isMember);

        return roomResponse;
    }
    public boolean isAlreadyJoined(User user, Room room) {
        // 假设有一个方法在你的 repository 中检查用户是否加入了房间
        return userRoomRepository.existsByUserAndRoom(user, room);
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





    public Blob convertToBlob(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        Blob blob = new SerialBlob(bytes);
        return blob;
    }


//    public String generateRoomCode(int length) {
//        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
//        StringBuilder roomCode = new StringBuilder(length);
//        Random random = new Random();
//
//        for (int i = 0; i < length; i++) {
//            int index = random.nextInt(characters.length());
//            roomCode.append(characters.charAt(index));
//        }
//        if(roomRepository.existsByRoomCode(roomCode.toString())) {
//            return generateRoomCode(length);
//        }
//
//        return roomCode.toString();
//    }
    public String hashRoomCode(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found.", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    private RoomResponse buildRoomResponse(Room room) {
        if (room == null) {
            return null;
        }
        RoomResponse response = new RoomResponse();
        response.setRoomCode(room.getRoomCode());
        response.setTitle(room.getTitle());
        response.setDescription(room.getDescription());
        response.setRoomType(room.getRoomType());
        response.setCreateTime(room.getCreateTime());
        response.setImage(blobToBase64String(room.getImage()));
        return response;
    }

    public RoomResponse findByRoomCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode);
        System.out.println(room);
        return buildRoomResponse(room);  // 假設在這個方法中包含 dbRoomFiles
    }

    public RoomResponse accessRoom(String roomCode, String password) {
        Room room = roomRepository.findByRoomCodeAndPassword(roomCode, password);
        if (room == null) {
            throw new RoomNotFoundException("Room with the password is Incorrect.");
        }
        return buildRoomResponse(room);  // 假設在這個方法中不包含 dbRoomFiles
    }

    public Room findByRoomCode1(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode);
        System.out.println(room);
        return room;  // 假設在這個方法中包含 dbRoomFiles
    }


    public void joinRoom(User user, Room room) {
        UserRoom userRoom = new UserRoom();
        userRoom.setUser(user);
        userRoom.setRoom(room);
        userRoom.setJoinedAt(LocalDateTime.now());
        userRoomRepository.save(userRoom);
    }



}
