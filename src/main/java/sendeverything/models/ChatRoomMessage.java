package sendeverything.models;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document
public class ChatRoomMessage {
    @Id
    private String id;
    private MessageType type;
    private String content;
    private String sender;
    private String roomCode;
    private Integer userCurrentCount;
    @Indexed(direction = IndexDirection.DESCENDING)
    private LocalDateTime timestamp;  // 自動設置當前時間為消息時間

    // 省略其他屬性的getter和setter
}