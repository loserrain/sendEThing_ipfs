package sendeverything.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sendeverything.models.Message;
import sendeverything.models.User;
import sendeverything.payload.request.CodeRequest;
import sendeverything.payload.request.MessageRequest;
import sendeverything.payload.response.FileNameResponse;
import sendeverything.repository.MessageRepository;
import sendeverything.repository.UserRepository;

import sendeverything.service.MessageService;

import java.security.Principal;
import java.util.Optional;
@CrossOrigin(origins = "*", maxAge = 3600)
//@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class MessageController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @PostMapping("/uploadMessage")
    public ResponseEntity<?> uploadMessage(@RequestBody MessageRequest content, Principal principal, HttpServletRequest request){

        String messageContent = content.getContent();
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Content cannot be empty");
        }
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
        try {
            Message message = messageService.saveMessage(messageContent, optionalUser);
            FileNameResponse fileNameResponse = messageService.fileNameResponse(message);
            return ResponseEntity.ok(fileNameResponse);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Cannot save empty content");
        }

    }

    @PostMapping("/getMessage")
    public ResponseEntity<?> getMessage(@RequestBody CodeRequest verificationCode) {
        String code= verificationCode.getCode();

        Message message = messageService.getMessage(code);
        FileNameResponse fileNameResponse = messageService.fileMessageResponse(message);
        return ResponseEntity.ok(fileNameResponse);
    }



}
