package sendeverything.payload.response;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

import java.sql.Blob;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoResponse {
	private Long id;
	private String username;
	private String email;
	private List<String> roles;
	private String provider;
	private String imgUrl;
	private String profileImage;
	private String accessToken;
	private String refreshToken;
	private ResponseCookie jwtCookie;
	private ResponseCookie RefreshTokenCookie;
}
