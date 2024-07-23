package sendeverything.payload.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatKeyAndVectorResponse {
    private String roomCode;
    private String UserPublicKey;
    private String UserPrivateKey;
    private String roomVector;

}
