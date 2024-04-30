package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sendeverything.models.DatabaseFile;
import sendeverything.models.FileNameAndVerifyCodeProjection;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.DBRoomFileChunk;
import sendeverything.models.room.Room;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DBRoomFileRepository extends JpaRepository<DBRoomFile, Long> {
    @Query("SELECT df.fileName FROM DatabaseFile df WHERE df.user.id = :userId ORDER BY df.timestamp DESC")
    List<String> findFileNamesByUserIdOrderByTimestampDesc(@Param("userId") Long userId);
    Optional<DBRoomFile>findByFileIdAndRoom_RoomCode(String fileId, String roomCode);
    Optional<DBRoomFile> findByFileId(String fileId);
    Optional<DBRoomFile> findByVerificationCode(String verificationCode);
    List<DBRoomFile>findAllByRoom(Room room);







}
