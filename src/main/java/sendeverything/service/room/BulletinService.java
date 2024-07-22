package sendeverything.service.room;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.exception.RoomNotFoundException;
import sendeverything.models.ChatRoomMessage;
import sendeverything.models.User;
import sendeverything.models.room.*;
import sendeverything.payload.response.RoomResponse;
import sendeverything.repository.ChatRoomMessageRepository;
import sendeverything.repository.RoomRepository;
import sendeverything.repository.UserRepository;
import sendeverything.repository.UserRoomRepository;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
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
    private final ChatRoomMessageRepository chatRoomMessageRepository;

    @Autowired
    public BulletinService(RoomRepository roomRepository, UserRepository userRepository, UserRoomRepository userRoomRepository, ChatRoomMessageRepository chatRoomMessageRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.userRoomRepository = userRoomRepository;
        this.chatRoomMessageRepository = chatRoomMessageRepository;
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

    public String saveRoom(String title, String roomDescription , String roomPassword , MultipartFile roomImage , Optional<User> user, RoomType roomType, BoardType boardType, String roomPrime, String initVector) throws Exception {
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime newTime = createTime.plusHours(8);
        Blob image = convertToBlob(roomImage);
        String roomCode = generateRoomCode();

        Room room = new Room(roomCode,title,roomDescription,roomPassword,image,roomType,boardType,newTime, roomPrime, initVector);
        user.ifPresent(room::setOwner);
        roomRepository.save(room);

        return room.getRoomCode();
    }

    public String saveSecretRoom(String title, String roomDescription , String roomPassword , Blob roomImage , Optional<User> user, RoomType roomType, BoardType boardType, String roomPrime, String initVector) throws Exception {
        LocalDateTime createTime = LocalDateTime.now();
        LocalDateTime newTime = createTime.plusHours(8);
        String roomCode = generateRoomCode();


        Room room = new Room(roomCode,title,roomDescription,roomPassword,roomImage,roomType,boardType,newTime, roomPrime, initVector);
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


    public void joinRoom(User user, Room room, String userPublicKey, String userPrivateKey) {
        UserRoom userRoom = new UserRoom();
        userRoom.setUser(user);
        userRoom.setRoom(room);
        userRoom.setJoinedAt(LocalDateTime.now());
        userRoom.setUserPublicKey(userPublicKey);
        userRoom.setUserPrivateKey(userPrivateKey);
        userRoom.setUserCount(1);
        userRoomRepository.save(userRoom);
    }

    @Transactional
    public void joinRoomAndCountJudge(User user, Room room, String userPublicKey, String userPrivateKey, String roomCode) {
        joinRoom(user, room, userPublicKey, userPrivateKey);
        userCountJudge(roomCode);
//        calChatMessageSharedKey(user, roomCode);
    }

    public void userCountJudge(String roomCode) {
        List<Integer> userCountList = userRoomRepository.findUserCountByRoomCode(roomCode);

        int totalUserCount = userCountList.size();
//        for(Integer userCount : userCountList) {
//            totalUserCount += userCount;
//        }
        System.out.println("Total user count: " + totalUserCount);
        if(totalUserCount > 2) {
            calculateSharedKey(roomCode);
        } else if (totalUserCount == 2) {
//            將用戶A的共享密鑰設定為用戶B的公鑰，將用戶B的共享密鑰設定為用戶A的公鑰
            System.out.println("Swapping user shared keys...");
            swapUserSharedKeys(roomCode);
        } else if (totalUserCount == 1) {
            saveRoomSharedKey(roomCode);
        }
    }

    public void saveRoomSharedKey(String roomCode) {
        List<UserRoom> userRoomList = userRoomRepository.findUsersByRoomCode(roomCode);
        System.out.println("userRoomList: " + userRoomList);
        userRoomList.get(0).setUserSharedKey(userRoomList.get(0).getUserPrivateKey());
    }

    public void swapUserSharedKeys(String roomCode) {
        List<KeyPairDTO> keyPairDTOList = userRoomRepository.findKeyPairDTOByRoomCode(roomCode);
        if (keyPairDTOList.size() == 2) {
            // 获取两个用户的公钥
            BigInteger publicKeyA = new BigInteger(keyPairDTOList.get(0).getPublicKey());
            BigInteger publicKeyB = new BigInteger(keyPairDTOList.get(1).getPublicKey());
            // 获取用户对应的 UserRoom 实体
            List<UserRoom> userRoomList = userRoomRepository.findUsersByRoomCode(roomCode);
            UserRoom userRoomA = userRoomList.get(0);
            UserRoom userRoomB = userRoomList.get(1);
            System.out.println("userRoomA: " + userRoomA);
            System.out.println("userRoomB: " + userRoomB);
            // 交换公钥作为共享密钥
            if (userRoomA != null && userRoomB != null) {
                userRoomA.setUserSharedKey(publicKeyB.toString());
                userRoomB.setUserSharedKey(publicKeyA.toString());

                // 保存更新
                userRoomRepository.save(userRoomA);
                userRoomRepository.save(userRoomB);
            } else {
                // 处理找不到 UserRoom 实体的情况
                System.err.println("Error: One or both UserRoom entries not found.");
            }
        } else {
            System.err.println("Error: The number of users in the room is not exactly 2.");
        }
    }

    public void calculateSharedKey(String roomCode) {
        List<UserRoom> userRoomList = userRoomRepository.findUsersByRoomCode(roomCode);
        List<KeyPairDTO> keyPairDTOList= userRoomRepository.findKeyPairDTOByRoomCode(roomCode);
        BigInteger p = new BigInteger(roomRepository.findByRoomCode(roomCode).getRoomPrime());
        List<BigInteger> publicKeyBigIntList = new ArrayList<>();
        List<BigInteger> privateKeyBigIntList = new ArrayList<>();

        for (KeyPairDTO keyPairDTO : keyPairDTOList) {
            BigInteger publicKeyBigInt = new BigInteger(keyPairDTO.getPublicKey());
            publicKeyBigIntList.add(publicKeyBigInt);

            BigInteger privateKeyBigInt = new BigInteger(keyPairDTO.getPrivateKey());
            privateKeyBigIntList.add(privateKeyBigInt);
        }

        for(int i = 0; i < keyPairDTOList.size(); i++) {
            BigInteger result = BigInteger.ONE;

            for(int j = i; j < i + keyPairDTOList.size(); j++) {
                if( i != j) {
                    if (j == i + 1) {
                        int nextIndex = (i + 1) % keyPairDTOList.size();
                        int nextIndex2 = (i + 2) % keyPairDTOList.size();
                        result = publicKeyBigIntList.get(nextIndex);
                        result = result.modPow(privateKeyBigIntList.get(nextIndex2), p);
                    } else {
                        int index = (j) % keyPairDTOList.size();
                        int index2 = (i + 2) % keyPairDTOList.size();
                        if(index != i & index != index2) {
                            result = result.modPow(privateKeyBigIntList.get(index), p);
                        }
                    }
                }
            }
            System.out.println("ownnnnnnnnnnnnnnnnnner: " + keyPairDTOList.get(i).getUserId());
            System.out.println("Shared secret key between: " + result.modPow(privateKeyBigIntList.get(i), p));
            UserRoom userRoomToUpdata = userRoomList.get(i);
            if(userRoomToUpdata != null) {
                userRoomToUpdata.setUserSharedKey(result.toString());
                userRoomRepository.save(userRoomToUpdata);
            }
        }

    }

    public List<Integer> removeDuplicates(List<Integer> list) {
        return list.stream().distinct().collect(Collectors.toList());
    }

    public Map<Integer, String> calChatMessageSharedKey(User user, String roomCode) {
//        找出該位用戶在房間中的索引
        int userIndex = findUserIndexInRoom(user, roomCode);
        List<KeyPairDTO> keyPairDTOList= userRoomRepository.findKeyPairDTOByRoomCode(roomCode);
        List<ChatRoomMessage> chatMessageUser = chatRoomMessageRepository.findUserCountByRoomCode(roomCode);
        List<Integer> chatMessageUserCountList = new ArrayList<>();

        for (ChatRoomMessage chatRoomMessage : chatMessageUser) {
            chatMessageUserCountList.add(chatRoomMessage.getUserCurrentCount());
        }
        chatMessageUserCountList = removeDuplicates(chatMessageUserCountList);

        System.out.println("userIndex: " + userIndex);

        BigInteger p = new BigInteger(roomRepository.findByRoomCode(roomCode).getRoomPrime());
        List<BigInteger> publicKeyBigIntList = new ArrayList<>();
        List<BigInteger> privateKeyBigIntList = new ArrayList<>();
        Map<Integer, String> sharedKeyMap = new HashMap<>();

        for (KeyPairDTO keyPairDTO : keyPairDTOList) {
            BigInteger publicKeyBigInt = new BigInteger(keyPairDTO.getPublicKey());
            publicKeyBigIntList.add(publicKeyBigInt);

            BigInteger privateKeyBigInt = new BigInteger(keyPairDTO.getPrivateKey());
            privateKeyBigIntList.add(privateKeyBigInt);
        }

//        userIndex = 1; chatMessageUserCountList = [1, 2, 3, 4]
        for(Integer chatMessageUserCount : chatMessageUserCountList) {
            int chatMessageUserIndex = chatMessageUserCount;
            if(userIndex < chatMessageUserIndex && chatMessageUserIndex > 1) {
                System.out.println("User count: " + chatMessageUserIndex);

                BigInteger result = BigInteger.ONE;
//                userIndex = 0; chatMessageUserIndex = 4

                for(int j = userIndex; j < userIndex + chatMessageUserIndex; j++) {
                    if(userIndex != j) {
                        if (j == userIndex + 1) {
                            int nextIndex = (userIndex + 1) % chatMessageUserIndex;
                            int nextIndex2 = (userIndex + 2) % chatMessageUserIndex;
                            result = publicKeyBigIntList.get(nextIndex);
                            result = result.modPow(privateKeyBigIntList.get(nextIndex2), p);
                        } else {
                            int index = (j) % chatMessageUserIndex;
                            int index2 = (userIndex + 2) % chatMessageUserIndex;
                            if(index != userIndex & index != index2) {
                                result = result.modPow(privateKeyBigIntList.get(index), p);
                            }
                        }
                    }
                }

                sharedKeyMap.put(chatMessageUserIndex, result.toString());
                System.out.println("ownnnnnnnnnnnnnnnnnner: " + keyPairDTOList.get(userIndex).getUserId());
                System.out.println("Shared secret key between: " + sharedKeyMap);
            }

        }

        return sharedKeyMap;
    }

    public int findUserIndexInRoom(User user, String roomCode) {
        List<UserRoom> userRoomList = userRoomRepository.findUsersByRoomCode(roomCode);
        for (int i = 0; i < userRoomList.size(); i++) {
            if (userRoomList.get(i).getUser().equals(user)) {
                return i;  // 返回用户在列表中的索引
            }
        }
        return -1;  // 如果未找到用户，返回-1
    }

}
