package sendeverything.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoomRequest {
    @NotBlank
    String roomCode;
    @NotBlank
    String password;
    String roomType;
    String userPublicKey;
    String userPrivateKey;
}
