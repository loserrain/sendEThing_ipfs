package sendeverything.security.services;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sendeverything.models.ERole;
import sendeverything.models.Provider;
import sendeverything.models.Role;
import sendeverything.models.User;
import sendeverything.payload.request.LoginRequest;
import sendeverything.payload.request.SignupRequest;
import sendeverything.payload.response.MessageResponse;
import sendeverything.payload.response.UserInfoResponse;
import sendeverything.security.jwt.AuthTokenFilter;
import sendeverything.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import sendeverything.repository.RoleRepository;
import sendeverything.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor// 會自動生成一個構造器，用於對所有final和@NonNull字段進行初始化，會有依賴注入的效果，不需要顯式地使用@Autowired注入
public class AuthenticationService {


    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    private final WebClient webClient;




    public String getProfileImageBase64(String username) throws SQLException, IOException {
//        System.out.println(username);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User Not Found with username: " + username));
        if (user.getImgUrl() != null) {
            return getImageAsBase64(user.getImgUrl()).block();
        }else{
            return convertBlobToBase64(user.getProfileImage());
        }

    }






    public Mono<String> getImageAsBase64(String imageUrl) {
        return webClient.get()
                .uri(imageUrl)
                .retrieve()
                .bodyToMono(byte[].class)
                .map(bytes -> Base64.getEncoder().encodeToString(bytes)); // Convert to Base64 string
    }

    public UserInfoResponse processOAuthPostLogin( String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User Not Found with username: " + username));
        String jwtToken = jwtUtils.generateTokenFromUsername(username);
        String profileImageBase64 =getImageAsBase64(user.getImgUrl()).block();


        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(jwtToken)
                .profileImageBase64(profileImageBase64)
                .build();
    }

    public Boolean googleUser_inLocal(String email) {
        return userRepository.findProviderByEmail(email);
    }

    public Optional<User> processGoogleUserInLocal(String email) {
        return userRepository.findByEmail(email);
    }





    public void saveGoogleUser(Authentication authentication, String username, String email,String imgUrl) {


        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(email);
                    newUser.setPassword("");
                    newUser.setProvider(Provider.GOOGLE);
                    newUser.setImgUrl(imgUrl);
                    System.out.println("save user");
                    return userRepository.save(newUser);
                });
    }





    public UserInfoResponse authenticate(LoginRequest loginRequest) throws SQLException, IOException {
        Authentication authentication = authenticationManager // AuthenticationManager是一個介面，它只有一個方法authenticate，用來驗證傳入的Authentication物件是否有效。
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())); // UsernamePasswordAuthenticationToken是Authentication的實作類別，用來儲存使用者的帳號密碼。
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String username= userDetails.getUsername();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(username);// 產生cookie
        ResponseCookie refreshTokenCookie = jwtUtils.createRefreshTokenCookie(username);

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());



        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String profileImageBase64 = convertBlobToBase64(userDetails.getProfileImage());

        return UserInfoResponse.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .profileImageBase64(profileImageBase64)
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .jwtCookie(jwtCookie)
                .RefreshTokenCookie(refreshTokenCookie)
                .build();
    }
    public String convertBlobToBase64(Blob imageBlob) throws SQLException, IOException {
        InputStream inputStream = imageBlob.getBinaryStream();
        byte[] bytes = inputStream.readAllBytes();
        return Base64.getEncoder().encodeToString(bytes);
    }

    public  Blob convertBase64ToBlob(String base64String) throws SQLException {
        byte[] bytes = Base64.getDecoder().decode(base64String);
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            // 创建Blob对象
            Blob blob = new javax.sql.rowset.serial.SerialBlob(bytes);
            return blob;
        } catch (Exception e) {
            throw new SQLException("Error converting Base64 string to Blob: " + e.getMessage());
        }
    }

    public Blob convertToBlob(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        Blob blob = new SerialBlob(bytes);
        return blob;
    }
    public MessageResponse register(MultipartFile file,String username,String email,String password) throws Exception {
        Blob image = convertToBlob(file);

        if (userRepository.existsByUsername(username)) {
            return new MessageResponse("Error: Username is already taken!");
        }
        User user = new User(username,
                email,
                encoder.encode(password),
                Provider.LOCAL,
                image);

//        Set<String> strRoles = signUpRequest.getRole();
//        Set<Role> roles = new HashSet<>();
//
//        if (strRoles == null) {
//            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
//                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
//            roles.add(userRole);
//        } else {
//            strRoles.forEach(role -> {
//                switch (role) {
//                    case "admin":
//                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
//                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
//                        roles.add(adminRole);
//
//                        break;
//                    case "mod":
//                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
//                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
//                        roles.add(modRole);
//
//                        break;
//                    default:
//                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
//                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
//                        roles.add(userRole);
//                }
//            });


//        user.setRoles(roles);
        userRepository.save(user);
        return new MessageResponse("User registered successfully!");
    }

    public ResponseCookie logoutUser() {

        return jwtUtils.getCleanJwtCookie();
    }
    public void clearCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                cookie.setValue(null);
                cookie.setPath("/"); // 確保設定相同的路徑
                cookie.setMaxAge(0); // 將最大壽命設置為0，以立即刪除cookie
                response.addCookie(cookie);
            }
        }
    }




    public ResponseEntity<?> checkAuth(HttpServletRequest request, HttpServletResponse response) {
        try {
            String jwt = jwtUtils.getJwtFromCookies(request);
            System.out.println("jwt: "+jwt);
            jwtUtils.validateJwtToken(jwt);  // Validates and throws exceptions if valid
            Optional<User> user = userRepository.findByUsername(jwtUtils.getUserNameFromJwtToken(jwt));
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Error: User not found!"));
            }
            return ResponseEntity.ok().body(UserInfoResponse.builder().
                    id(user.get().getId()).
                    username(user.get().getUsername()).
                    email(user.get().getEmail()).
                    accessToken(jwt).
                    build());
        } catch (ExpiredJwtException e) {
            // Try using Refresh Token to issue a new JWT
            String refreshToken = jwtUtils.getRefreshTokenFromCookies(request);
            System.out.println("ExpiredJwtException: "+refreshToken);
            if (refreshToken != null) {
                try {
                    jwtUtils.validateJwtToken(refreshToken);  // Validate refresh token


                    Optional<User> user = userRepository.findByUsername(jwtUtils.getUserNameFromJwtToken(refreshToken));
                    if (user.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Error: User not found!"));
                    }// Retrieve user details from token or database
                    String newJwt = jwtUtils.generateTokenFromUsername(user.get().getUsername());

                    String newRefreshToken = jwtUtils.generateRefreshToken(user.get().getUsername());  // Optionally regenerate the refresh token
                    System.out.println("newRefreshToken: "+newRefreshToken);
                    // Update client's JWT and Refresh Token in cookies
                    ResponseCookie newJwtCookie = jwtUtils.generateJwtCookie(newJwt);
                    ResponseCookie newRefreshTokenCookie = jwtUtils.createRefreshTokenCookie(newRefreshToken);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.SET_COOKIE, newJwtCookie.toString());
                    headers.add(HttpHeaders.SET_COOKIE, newRefreshTokenCookie.toString());

                    return ResponseEntity.ok().headers(headers).body(UserInfoResponse.builder().
                            id(user.get().getId()).
                            provider(user.get().getProvider().toString()).
                            username(user.get().getUsername()).
                            email(user.get().getEmail()).
                            accessToken(newJwt).

                            build());
                } catch (Exception ex) {
                    logger.error("Refresh Token Validation Failed: {}", ex.getMessage());
                }
            }
        } catch (Exception ex) {
            logger.error("JWT Validation Failed: {}", ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("Error: Login failed!"));
    }


}