package sendeverything.payload.request;

import lombok.Data;

@Data
public class SpeedTestRequest {
    private String fileName;
    private String speed;
}
