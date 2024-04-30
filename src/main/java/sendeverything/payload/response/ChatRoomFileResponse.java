package sendeverything.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sendeverything.models.room.BoardType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomFileResponse {
    BoardType boardType;
    List<FileNameResponse> fileNameResponse;
}
