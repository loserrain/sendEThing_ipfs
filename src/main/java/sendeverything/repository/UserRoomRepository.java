package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sendeverything.models.User;
import sendeverything.models.room.KeyPairDTO;
import sendeverything.models.room.Room;
import sendeverything.models.room.UserRoom;

import java.util.List;

public interface UserRoomRepository extends JpaRepository<UserRoom, Long> {
    boolean existsByUserAndRoom(User user, Room room);
//    List<Room> findByUser(User user);
@Query("SELECT ur.room.roomCode FROM UserRoom ur WHERE ur.user = :user")
List<String> findRoomCodesByUser(User user);

@Query("SELECT ur FROM UserRoom ur WHERE ur.room.roomCode = :roomCode")
List<UserRoom> findUsersByRoomCode(String roomCode);

@Query("SELECT ur.userCount FROM UserRoom ur WHERE ur.room.roomCode = :roomCode")
List<Integer> findUserCountByRoomCode(String roomCode);

@Query("SELECT new sendeverything.models.room.KeyPairDTO(ur.user.id, ur.userPublicKey, ur.userPrivateKey) FROM UserRoom ur WHERE ur.room.roomCode = :roomCode")
List<KeyPairDTO> findKeyPairDTOByRoomCode(String roomCode);

@Query("SELECT ur FROM UserRoom ur WHERE ur.room.roomCode = :roomCode AND ur.user = :user")
UserRoom findUserByRoomCodesAndUser(String roomCode, User user);

@Query("SELECT ur FROM UserRoom ur WHERE ur.user = :user AND ur.userSharedKey IS NOT NULL")
List<UserRoom> findByUser(User user);

    @Query("SELECT ur FROM UserRoom ur WHERE ur.user = :user AND ur.room.roomType = 'Secret' AND ur.userSharedKey IS NOT NULL")
    List<UserRoom> findByUserAndSecretRoomType(User user);

    @Query("SELECT ur FROM UserRoom ur WHERE ur.user = :user AND ur.room.roomType != 'Secret' AND ur.userSharedKey IS NOT NULL")
    List<UserRoom> findByUserAndNotSecretRoomType(User user);

}
