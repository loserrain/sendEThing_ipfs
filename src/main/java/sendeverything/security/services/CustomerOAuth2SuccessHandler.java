package sendeverything.security.services;

import sendeverything.models.User;
import sendeverything.security.jwt.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomerOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private void setupResponseAndRedirect (HttpServletResponse response, String jwtToken, ResponseCookie jwtCookie,ResponseCookie refreshTokenCookie) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.addHeader("Authorization", "Bearer " + jwtToken);
        response.addHeader("Set-Cookie", jwtCookie.toString());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());
        response.setStatus(HttpServletResponse.SC_OK);
        response.sendRedirect("https://imbig404.com/checkGoogle");
    }
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        System.out.println(authentication);
        DefaultOidcUser oidcUser = (DefaultOidcUser) authentication.getPrincipal();
        String username = oidcUser.getEmail().split("@")[0];
        String email = oidcUser.getEmail();
        String imgUrl=oidcUser.getPicture();
        System.out.println(imgUrl);

        if(authenticationService.googleUser_inLocal(email)) {
            Optional<User> optionalUser=authenticationService.processGoogleUserInLocal(email);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                String jwtToken = jwtUtils.generateTokenFromUsername(user.getUsername());
                ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(username);
                ResponseCookie refreshTokenCookie = jwtUtils.createRefreshTokenCookie(username);
                setupResponseAndRedirect(response, jwtToken, jwtCookie,refreshTokenCookie);

            }
        }else {
            authenticationService.saveGoogleUser(authentication, username, email, imgUrl);
            String jwtToken = jwtUtils.generateTokenFromUsername(username);
            ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(username);
            ResponseCookie refreshTokenCookie = jwtUtils.createRefreshTokenCookie(username);
            setupResponseAndRedirect(response, jwtToken, jwtCookie,refreshTokenCookie);
        }








    }
    private final AuthenticationService authenticationService;
    private final JwtUtils jwtUtils;



    public CustomerOAuth2SuccessHandler(@Lazy AuthenticationService authenticationService, JwtUtils jwtUtils) {
        this.authenticationService = authenticationService;
        this.jwtUtils = jwtUtils;
    }

}
