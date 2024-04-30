package sendeverything.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import sendeverything.models.ChatRoomMessage;
import sendeverything.payload.request.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatRoomMessageRepository extends MongoRepository<ChatRoomMessage, String> {

    @Query("{ 'roomCode' : ?0, 'timestamp' : { $lt: ?1 } }")
    List<ChatRoomMessage> findMessagesBeforeDate(String roomCode, LocalDateTime lastTimestamp, Pageable pageable);

}
