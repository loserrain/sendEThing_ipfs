package sendeverything.payload.response;

import lombok.Data;

@Data

public class IsJoinedResponse {
    private boolean isJoined;
    private boolean isEncrypt;


    public IsJoinedResponse(boolean isJoined,boolean isEncrypt) {
        this.isJoined = isJoined;
        this.isEncrypt = isEncrypt;
    }
}
