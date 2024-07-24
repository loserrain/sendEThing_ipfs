package sendeverything.controllers;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import sendeverything.security.services.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sendeverything.payload.request.LoginRequest;
import sendeverything.payload.request.SignupRequest;
import sendeverything.payload.response.UserInfoResponse;
import sendeverything.payload.response.MessageResponse;
import sendeverything.repository.UserRepository;
import sendeverything.security.jwt.JwtUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;

//for Angular Client (withCredentials)
//@CrossOrigin(origins = "http://localhost:8080, http://localhost:8081, http://localhost:8080", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository userRepository;
  private final AuthenticationService service;
  private final JwtUtils jwtUtils;
  private final WebClient webClient;





  @GetMapping("/oauth2/redirect")
  public ResponseEntity<?> oauth2Redirect(@CookieValue(name = "sendEveryThing", required = false) String jwt) {
    if (jwt != null) {
      String username = jwtUtils.getUserNameFromJwtToken(jwt);
      UserInfoResponse userInfoResponse = service.processOAuthPostLogin(username);
      ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(username);
      System.out.println("username : " + username + " Login Success!");
      return ResponseEntity.ok()
              .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
              .body(userInfoResponse);
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Login failed!"));
    }
  }





  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) throws SQLException, IOException {

    UserInfoResponse userInfoResponse = service.authenticate(loginRequest);

    if(userInfoResponse != null) {

        ResponseCookie jwtCookie = userInfoResponse.getJwtCookie();
        ResponseCookie refreshTokenCookie = userInfoResponse.getRefreshTokenCookie();
        System.out.println("username : " + userInfoResponse.getUsername() + " Login Success!");

      return ResponseEntity.ok()
              .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
              .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(userInfoResponse);
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Login failed!"));
    }

  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@RequestParam("image") MultipartFile image,
                                        @RequestParam("username") String username,
                                        @RequestParam("email") String email,
                                        @RequestParam("password") String password
                                       ) throws Exception {
    if (userRepository.existsByUsername(username)) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(email)) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
    }// 以上兩個if判斷，若有重複的username或email，則回傳錯誤訊息

    // Create new user's account
    return ResponseEntity.ok(service.register(image, username, email, password));
  }

  @PostMapping("/signout")
  public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
    // 清除所有cookie
    service.clearCookies(request, response);

    // 清除JWT cookie并获取返回的cookie
    ResponseCookie jwtCookie = service.logoutUser();

    // 添加jwtCookie到响应头中
    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
            .body(new MessageResponse("You've been signed out!"));
  }
  @GetMapping("/checkAuth")
  public ResponseEntity<?> checkUser(HttpServletRequest request,HttpServletResponse response) {

    return service.checkAuth(request,response);
  }




//  @GetMapping("/a")
//  public String generateContent()throws Exception {
//    String input = "inputText";
////        FileProcessor fileProcessor = new FileProcessor();
////        String output = fileProcessor.generateContent(input);
//    System.out.println("inputText: "+input );
//    return input;
//  }
}
