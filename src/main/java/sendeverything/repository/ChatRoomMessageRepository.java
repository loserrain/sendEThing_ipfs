package sendeverything.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import sendeverything.models.ChatRoomMessage;
import sendeverything.payload.request.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRoomMessageRepository extends MongoRepository<ChatRoomMessage, String> {

    @Query("{ 'roomCode' : ?0, 'timestamp' : { $lt: ?1 } }")
    List<ChatRoomMessage> findMessagesBeforeDate(String roomCode, LocalDateTime lastTimestamp, Pageable pageable);

//    找出兩個時間前後資料
    @Query("{ 'roomCode' : ?0, 'timestamp' : { $lt: ?1 , '$gte': ?2} }")
    List<ChatRoomMessage> findMessagesDateAndDate(String roomCode, LocalDateTime lastTimestamp, LocalDateTime test, Pageable pageable);

    @Query("{ 'sender' : ?0, 'roomCode' : ?1 }")
    List<ChatRoomMessage> findUserBySenderAndRoomCode(String sender, String roomCode);

    @Query("{ 'roomCode' : ?0 }")
    List<ChatRoomMessage> findUserCountByRoomCode(String roomCode);
}
