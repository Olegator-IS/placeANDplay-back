package com.is.auth.api;

import com.is.auth.model.ResponseAnswers.Response;
import com.is.auth.model.ResponseAnswers.ResponseToken;
import com.is.auth.model.locations.CitiesDTO;
import com.is.auth.model.locations.CountriesDTO;
import com.is.auth.model.sports.SkillsDTO;
import com.is.auth.model.sports.SportsDTO;
import com.is.auth.model.user.LoginRequest;
import com.is.auth.model.user.RegistrationAddInfoRequest;
import com.is.auth.model.user.RegistrationRequest;
import com.is.auth.model.user.UserService;
import com.is.auth.service.EmailService;
import com.is.events.service.WebSocketService;
import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.mail.MessagingException;
import java.util.List;

@Api(tags = "Available APIs for the IDP", description = "List of methods for interacting with IDP")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/auth")
public class RbsControllerAuth {

    private static final Logger log = LoggerFactory.getLogger(RbsControllerAuth.class);
    private final UserService userService;
    private final EmailService emailService;
    private final WebSocketService webSocketService;

    @Autowired
    public RbsControllerAuth(UserService userService, EmailService emailService, WebSocketService webSocketService) {
        log.info("RbsControllerAuth initialized!");
        this.userService = userService;
        this.emailService = emailService;
        this.webSocketService = webSocketService;
    }

//    public void notifyEventUpdate(Long placeId) {
//        try {
//            webSocketService.notifyEventUpdate(placeId);
//        } catch (Exception e) {
//            log.error("Error sending WebSocket notification", e);
//        }
//    }

    // Этот метод вызывается при подключении клиента
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    // Этот метод вызывается при отключении клиента
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("Client disconnected from web socket");
    }

    @PostMapping("/registration")
    public ResponseEntity<?> registerUser(  @RequestAttribute("clientIp") String clientIp,
                                            @RequestAttribute("url") String url,
                                            @RequestAttribute("method") String method,
                                            @RequestAttribute("Request-Id") String requestId,
                                            @RequestAttribute("startTime") long startTime,
                                            @RequestHeader(value = "language", required = true) String language,
                                            @RequestBody RegistrationRequest registrationRequest) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        return userService.registrationRequest(clientIp,url,method,requestId,currentTime,executionTime,language,registrationRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestAttribute("clientIp") String clientIp,
                                       @RequestAttribute("url") String url,
                                       @RequestAttribute("method") String method,
                                       @RequestAttribute("Request-Id") String requestId,
                                       @RequestAttribute("startTime") long startTime,
                                       @RequestHeader(value = "language", required = true) String language,
                                       @RequestHeader(value = "isUser", required = false, defaultValue = "true") Boolean isUser,
                                       @RequestBody LoginRequest loginRequest) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        System.out.println("test");
        return userService.loginRequest(clientIp,url,method,requestId,currentTime,executionTime,language,loginRequest,isUser);
    }

    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(
                                        @RequestHeader(value = "language", required = true) String language,
                                        @RequestHeader String accessToken,
                                        @RequestHeader String refreshToken) {
        validateLanguage(language);
        return userService.validateTokenAndGetSubject(accessToken,refreshToken,language);
    }

    @GetMapping("/getUserInfoRefreshToken")
    public ResponseEntity<?> getUserInfoRefreshToken(
                                                    @RequestHeader(value = "language", required = true) String language,
                                                    @RequestHeader String refreshToken) {
        validateLanguage(language);
        return userService.refreshToken(refreshToken,language);
    }

    @PatchMapping("/registrationAddInfo")
    public ResponseEntity<?> registrationAddInfo(  @RequestAttribute("clientIp") String clientIp,
                                                   @RequestAttribute("url") String url,
                                                   @RequestAttribute("method") String method,
                                                   @RequestAttribute("Request-Id") String requestId,
                                                   @RequestAttribute("startTime") long startTime,
                                                   @RequestHeader(value = "language", required = true) String language,
                                                   @RequestHeader String accessToken,
                                                   @RequestHeader String refreshToken,
                                                   @RequestBody RegistrationAddInfoRequest registrationAddInfoRequest) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        return userService.registrationAddInfo(clientIp,url,method,requestId,currentTime,executionTime,language,registrationAddInfoRequest);
    }

    @GetMapping("/getListOfSports")
    public List<SportsDTO> getListOfSports(
            @RequestAttribute("clientIp") String clientIp,
            @RequestAttribute("url") String url,
            @RequestAttribute("method") String method,
            @RequestAttribute("Request-Id") String requestId,
            @RequestAttribute("startTime") long startTime,
            @RequestHeader(value = "language", required = true) String language) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // This is needed for execution time calculation
        long executionTime = currentTime - startTime; // Request execution time


        return userService.getSportsByLanguage(language);
    }

    @GetMapping("/getAttrModelBySport")
    public String getAttrModel(
            @RequestAttribute("clientIp") String clientIp,
            @RequestAttribute("url") String url,
            @RequestAttribute("method") String method,
            @RequestAttribute("Request-Id") String requestId,
            @RequestAttribute("startTime") long startTime,
            @RequestParam int sportId,
            @RequestHeader(value = "language", required = true) String language) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // This is needed for execution time calculation
        long executionTime = currentTime - startTime; // Request execution time
        return userService.getAttrModelBySport(sportId,language);
    }

    @GetMapping("/getListOfSkills")
    public List<SkillsDTO> getListOfSkills(
            @RequestAttribute("clientIp") String clientIp,
            @RequestAttribute("url") String url,
            @RequestAttribute("method") String method,
            @RequestAttribute("Request-Id") String requestId,
            @RequestAttribute("startTime") long startTime,
            @RequestHeader(value = "language", required = true) String language) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // This is needed for execution time calculation
        long executionTime = currentTime - startTime; // Request execution time


        return userService.getListOfSkills(language);
    }

    @GetMapping("/getListOfCities")
    public List<CitiesDTO> getListOfCities(
            @RequestAttribute("clientIp") String clientIp,
            @RequestAttribute("url") String url,
            @RequestAttribute("method") String method,
            @RequestAttribute("Request-Id") String requestId,
            @RequestAttribute("startTime") long startTime,
            @RequestHeader(value = "language", required = true) String language,
            @RequestHeader(value = "countryCode", required = true) Integer countryCode,
            @RequestHeader String accessToken,
            @RequestHeader String refreshToken) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // This is needed for execution time calculation
        long executionTime = currentTime - startTime; // Request execution time


        return userService.getListOfCities(language,countryCode);
    }

    @GetMapping("/getListOfCounries")
    public List<CountriesDTO> getListOfCountries(
            @RequestAttribute("clientIp") String clientIp,
            @RequestAttribute("url") String url,
            @RequestAttribute("method") String method,
            @RequestAttribute("Request-Id") String requestId,
            @RequestAttribute("startTime") long startTime,
            @RequestHeader(value = "language", required = true) String language,
            @RequestHeader String accessToken,
            @RequestHeader String refreshToken) {
        validateLanguage(language);
        long currentTime = System.currentTimeMillis(); // This is needed for execution time calculation
        long executionTime = currentTime - startTime; // Request execution time


        return userService.getListOfCountries(language);
    }

    @GetMapping("/emailVerification")
    public ResponseEntity<?> sendCodeToMail(@RequestParam String email,
                                            @RequestHeader(value = "language", required = true) String language,
                                            @RequestHeader String accessToken,
                                            @RequestHeader String refreshToken) throws MessagingException {
        validateLanguage(language);
        return emailService.sendVerificationEmail(email,language);
    }

    @GetMapping("/verifyCode")
    public ResponseEntity<?> verifyCode(@RequestParam String email,
                                        @RequestParam int code,
                                        @RequestHeader(value = "language", required = true) String language,
                                        @RequestHeader String accessToken,
                                        @RequestHeader String refreshToken) throws MessagingException {
        validateLanguage(language);
        return emailService.verifyCode(email,code,language);
    }
    @GetMapping("/testSystem")
    public ResponseEntity<ResponseToken> getUserInfo(){
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/upload-profile-picture")
    @ApiOperation(value = "Upload profile picture", notes = "Upload a profile picture for the user")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully uploaded profile picture"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<Response> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestHeader(value = "language", defaultValue = "ru") String language) {
        return userService.uploadProfilePicture(file, userId);
    }

    private void validateLanguage(String language) {
        if (!language.equals("ru") && !language.equals("en") && !language.equals("uz")) {
            throw new IllegalArgumentException("Unsupported language. Supported languages are: ru, en, uz");
        }
    }
}

record EventUpdateMessage(Long placeId) {
    // Можно добавить дополнительные поля, если нужно передавать больше информации
}