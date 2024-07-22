package sendeverything.payload.response;

import lombok.Data;
import sendeverything.models.ChatRoomMessage;
@Data
public class ChatImageDTO {
    private ChatRoomMessage chatRoomMessage;
    private String senderImage;  // 发送者的头像URL
    private IsJoinedResponse isJoinedResponse;
    public ChatImageDTO(ChatRoomMessage chatRoomMessage, String senderImage) {
        this.chatRoomMessage = chatRoomMessage;
        this.senderImage = senderImage;

    }
    public ChatImageDTO(ChatRoomMessage chatRoomMessage, String senderImage, IsJoinedResponse isJoinedResponse) {
        this.chatRoomMessage = chatRoomMessage;
        this.senderImage = senderImage;
        this.isJoinedResponse = isJoinedResponse;
    }

    public ChatImageDTO(IsJoinedResponse isJoinedResponse) {
        this.isJoinedResponse = isJoinedResponse;
    }


}
