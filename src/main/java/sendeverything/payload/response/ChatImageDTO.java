package sendeverything.payload.response;

import lombok.Data;
import sendeverything.models.ChatRoomMessage;
@Data
public class ChatImageDTO {
    private ChatRoomMessage chatRoomMessage;
    private String senderImage;  // 发送者的头像URL
    public ChatImageDTO(ChatRoomMessage chatRoomMessage, String senderImage) {
        this.chatRoomMessage = chatRoomMessage;
        this.senderImage = senderImage;
    }
}
