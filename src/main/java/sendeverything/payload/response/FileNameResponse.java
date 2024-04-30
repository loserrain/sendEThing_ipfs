package sendeverything.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileNameResponse {
    String fileName;
    String verificationCode;
    long fileSize;
    String content;
    LocalDateTime createdAt;
    String timeLeft;



    public FileNameResponse(String verificationCode){
        this.verificationCode = verificationCode;

    }
    public FileNameResponse(String name, String verificationCode, long size,  LocalDateTime createdAt,String timeLeft){
        this.fileName = name;
        this.verificationCode = verificationCode;
        this.fileSize = size;
        this.createdAt = createdAt;
        this.timeLeft = timeLeft;
    }
    public FileNameResponse(String name, String verificationCode, long size,  LocalDateTime createdAt){
        this.fileName = name;
        this.verificationCode = verificationCode;
        this.fileSize = size;
        this.createdAt = createdAt;

    }

}
