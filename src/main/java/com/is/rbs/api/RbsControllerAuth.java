package com.is.rbs.api;

import com.is.rbs.model.ResponseAnswers.ResponseToken;
import com.is.rbs.model.user.LoginRequest;
import com.is.rbs.model.user.RegistrationAddInfoRequest;
import com.is.rbs.model.user.RegistrationRequest;
import com.is.rbs.model.user.UserService;
import com.is.rbs.repository.UserRepository;
import io.swagger.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.is.rbs.model.ResponseAnswers.Response;


@Api(tags = "Available APIs for the IDP", description = "List of methods for interacting with IDP")

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/auth")
public class RbsControllerAuth {


    private static final Logger log = LoggerFactory.getLogger(RbsControllerAuth.class);
    private final UserService userService;


    @Autowired
    public RbsControllerAuth(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/registration")
    public ResponseEntity<?> registerUser(  @RequestAttribute("clientIp") String clientIp,
                                            @RequestAttribute("url") String url,
                                            @RequestAttribute("method") String method,
                                            @RequestAttribute("Request-Id") String requestId,
                                            @RequestAttribute("startTime") long startTime,
                                            @RequestBody RegistrationRequest registrationRequest) {
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        return userService.registrationRequest(clientIp,url,method,requestId,currentTime,executionTime,registrationRequest);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestAttribute("clientIp") String clientIp,
                                       @RequestAttribute("url") String url,
                                       @RequestAttribute("method") String method,
                                       @RequestAttribute("Request-Id") String requestId,
                                       @RequestAttribute("startTime") long startTime,
                                       @RequestHeader(value = "isUser", required = false, defaultValue = "true") Boolean isUser,
                                       @RequestBody LoginRequest loginRequest) {
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        System.out.println("test");
        return userService.loginRequest(clientIp,url,method,requestId,currentTime,executionTime,loginRequest,isUser);
    }

    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@RequestHeader String accessToken,@RequestHeader String refreshToken) {
        return userService.validateTokenAndGetSubject(accessToken,refreshToken);
    }

    @GetMapping("/getUserInfoRefreshToken")
    public ResponseEntity<?> getUserInfoRefreshToken(@RequestHeader String refreshToken) {
        return userService.refreshToken(refreshToken);
    }

    @PatchMapping("/registrationAddInfo")
    public ResponseEntity<?> registrationAddInfo(  @RequestAttribute("clientIp") String clientIp,
                                                   @RequestAttribute("url") String url,
                                                   @RequestAttribute("method") String method,
                                                   @RequestAttribute("Request-Id") String requestId,
                                                   @RequestAttribute("startTime") long startTime,
                                                   @RequestBody RegistrationAddInfoRequest registrationAddInfoRequest) {
        long currentTime = System.currentTimeMillis(); // Это необходимо для время выполнения запроса
        long executionTime = currentTime - startTime; // Время выполнения запроса
        return userService.registrationAddInfo(clientIp,url,method,requestId,currentTime,executionTime,registrationAddInfoRequest);
    }

    @GetMapping("/testSystem")
    public ResponseEntity<ResponseToken> getUserInfo(){
        return new ResponseEntity<>(HttpStatus.OK);
    }

}