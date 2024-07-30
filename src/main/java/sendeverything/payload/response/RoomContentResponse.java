package sendeverything.payload.response;

import lombok.Data;
import sendeverything.payload.dto.DBRoomDTO;

import java.util.List;

@Data
public class RoomContentResponse {
    private RoomResponse roomResponse;
    private Boolean isRoomOwner;
    private List<DBRoomDTO> dbRoomFiles;
}
