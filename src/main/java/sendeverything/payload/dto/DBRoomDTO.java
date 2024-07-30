package sendeverything.payload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DBRoomDTO {
    private Long fileSize;
    private String fileName;
    private String description;
    private LocalDateTime timestamp;
    private String verificationCode;
    private String uploaderName;
}
