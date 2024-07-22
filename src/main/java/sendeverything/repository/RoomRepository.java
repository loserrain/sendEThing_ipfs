package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sendeverything.models.User;
import sendeverything.models.room.BoardType;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.Room;
import sendeverything.models.room.RoomType;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Boolean existsByRoomCode(String roomCode);
    Room findByRoomCodeAndPassword(String roomCode, String password);
    Room findByRoomCode(String roomCode);
    List<Room> findByOwner(User owner);
    List<Room>findByBoardType(BoardType boardType);


}
