package sendeverything.exception;

import lombok.Data;
import lombok.Getter;

@Getter
public class CustomJwtException extends Exception {
    private final JwtErrorType errorType;

    public CustomJwtException(String message, JwtErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

}

