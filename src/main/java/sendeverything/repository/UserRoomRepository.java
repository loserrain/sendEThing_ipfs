package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sendeverything.models.User;
import sendeverything.models.room.Room;
import sendeverything.models.room.UserRoom;

import java.util.List;

public interface UserRoomRepository extends JpaRepository<UserRoom, Long> {
    boolean existsByUserAndRoom(User user, Room room);
//    List<Room> findByUser(User user);
@Query("SELECT ur.room.roomCode FROM UserRoom ur WHERE ur.user = :user")
List<String> findRoomCodesByUser(User user);


}
