package sendeverything.models.room;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyPairDTO {
    private Long userId;
    private String publicKey;
    private String privateKey;
}
