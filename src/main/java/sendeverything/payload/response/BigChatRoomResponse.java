package sendeverything.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BigChatRoomResponse {
    private String chatRoomImage;
    private String chatRoomTitle;
    private String chatRoomMessage;
    private String chatRoomTime;
    private String chatRoomCode;
    private String chatRoomDescription;
    private String chatRoomType;
    private Integer chatRoomUserCount;
}
