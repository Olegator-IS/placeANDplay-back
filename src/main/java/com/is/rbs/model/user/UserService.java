package com.is.rbs.model.user;

import com.is.rbs.api.CustomAuthenticationProvider;
import com.is.rbs.config.JwtAuthenticationFilter;
import com.is.rbs.config.TokenSecurity;
import com.is.rbs.model.ResponseAnswers.Response;
import com.is.rbs.model.logger.Logger;
import com.is.rbs.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {

    private final SecretKey secretKey;

    @Autowired
    private Logger logger;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomAuthenticationProvider customAuthenticationProvider;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public UserService(SecretKey secretKey, JwtAuthenticationFilter jwtAuthenticationFilter,Logger logger) {
        this.secretKey = secretKey;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.logger = logger;
    }

    public User registerUser(String email, String password, String firstName, String lastName) {
        String passwordHash = passwordEncoder.encode(password);
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(false);
        user.setRegistrationDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    public ResponseEntity<Response> getToken(String email, String password) {
        Response response = new Response(200, "OK", "Успешно");
        try {
            Authentication authentication = customAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwtToken = jwtAuthenticationFilter.generateToken(email,authentication);
            String jwtRefreshToken = jwtAuthenticationFilter.generateRefreshToken(email,authentication);

            if (jwtToken.equals("Error")) {
                log.error("Ошибка генерации токена для email: {}", email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new Response(500, "TOKEN_GENERATION_ERROR", "Ошибка генерации токена"));
            }

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("accessToken", TokenSecurity.encryptToken(jwtToken, secretKey));
            tokenInfo.put("refreshToken", TokenSecurity.encryptToken(jwtRefreshToken, secretKey));
            response.setTokenInfo(tokenInfo);
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {

            log.warn("Ошибка аутентификации для email: {}. Причина: {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "INVALID_USERNAME_OR_PASSWORD", e.getMessage()));
        } catch (Exception e) {
            log.error("Неожиданная ошибка при генерации токена для email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response(500, "UNEXPECTED_ERROR", "Произошла ошибка на сервере"));
        }
    }

    public ResponseEntity<Response> validateTokenAndGetSubject(String accessToken, String refreshToken) {
        try {
            String decryptedToken = TokenSecurity.decryptToken(accessToken, secretKey);
            Claims claims = jwtAuthenticationFilter.extractClaims(decryptedToken);

            if (claims.getSubject() != null) {

                User user = !claims.getSubject().equals("GUEST")?
                        userRepository.getUserInfoByEmail((claims.getSubject())):null;
                Map<String, Object> additionalInfo = new HashMap<>();

                additionalInfo.put("userId", user!=null?user.getUserId():0);
                additionalInfo.put("firstName", user!=null?user.getFirstName():"GUEST");
                additionalInfo.put("lastName", user!=null?user.getLastName():"GUEST");
                additionalInfo.put("email", user!=null?user.getEmail():"GUEST");
                additionalInfo.put("role", user!=null?"USER":"GUEST");
                additionalInfo.put("isEmailVerified", user != null && user.isEmailVerified());
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new Response(200, "OK", additionalInfo));
            } else {
                log.warn("Пустой subject в токене. Токен: {}", accessToken);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Response(400, "TOKEN_VALIDATION_ERROR", "Произошла ошибка при получении данных из токена."));
            }

        } catch (ExpiredJwtException e) {
            log.warn("Токен истёк: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "TOKEN_EXPIRED", "Срок действия токена истёк"));
        } catch (JwtException e) {
            log.warn("Ошибка валидации токена: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "TOKEN_VALIDATION_ERROR", "Произошла ошибка при получении данных из токена."));
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при валидации токена: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response(500, "TOKEN_ERROR", "Непредвиденная ошибка"));
        }
    }

    public ResponseEntity<Response> refreshToken(String refreshToken) {
        Response response = new Response(200, "OK", "Успешно");
        try {
            String decryptedToken = TokenSecurity.decryptToken(refreshToken, secretKey);
            Claims claims = jwtAuthenticationFilter.extractClaims(decryptedToken);
            User user = userRepository.getUserInfoByEmail((claims.getSubject()));

            Authentication authentication = customAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPasswordHash())
            );
            String jwtToken = jwtAuthenticationFilter.generateToken(user.getEmail(), authentication);

            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("accessToken", TokenSecurity.encryptToken(jwtToken, secretKey));
            response.setTokenInfo(tokenInfo);
            return ResponseEntity.ok(response);

        } catch (ExpiredJwtException e) {
            log.warn("Refresh токен истёк: {}", refreshToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "REFRESH_TOKEN_EXPIRED", "Срок действия токена истёк"));
        } catch (JwtException e) {
            log.warn("Ошибка валидации refresh токена: {}", refreshToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "REFRESH_TOKEN_VALIDATION_ERROR", "Произошла ошибка при получении данных из токена."));
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при обработке refresh токена: {}", refreshToken, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response(500, "REFRESH_TOKEN_ERROR", "Непредвиденная ошибка"));
        }
    }

    public ResponseEntity<Response> registrationRequest(String clientIp,String url,String method,String requestId,
                                                        long currentTime,long executionTime,
                                                        RegistrationRequest registrationRequest) {
        if (userRepository.existsByEmail(registrationRequest.getEmail())) {
            Response response = new Response("EMAIL_IS_ALREADY_EXIST", "Email already in use",
                    HttpStatus.CONFLICT.value());
            logger.logRequestDetails(HttpStatus.CONFLICT,currentTime,method,url,requestId,clientIp,executionTime,registrationRequest,response);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        try {
            User user = registerUser(
                    registrationRequest.getEmail(),
                    registrationRequest.getPassword(),
                    registrationRequest.getFirstName(),
                    registrationRequest.getLastName()
            );

            Response response = new Response(HttpStatus.CREATED.value(), "USER_CREATED_SUCCESSFULLY", user.getUserId());
            logger.logRequestDetails(HttpStatus.CREATED,currentTime,method,url,requestId,clientIp,executionTime,registrationRequest,response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Response response = new Response(HttpStatus.BAD_REQUEST.value(), "SOMETHING_WRONG",
                    "Error during processing, try again");
            logger.logRequestDetails(HttpStatus.BAD_REQUEST,currentTime,method,url,requestId,clientIp,executionTime,registrationRequest,e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    public ResponseEntity<Response> loginRequest(String clientIp,String url,String method,String requestId,
                                                 long currentTime,long executionTime,LoginRequest loginRequest,
                                                 boolean isUser) {
        if (!userRepository.existsByEmail(loginRequest.getEmail())&&isUser) {
            Response response = new Response("EMAIL_IS_NOT_FOUND", "Required email is not found",
                    HttpStatus.UNAUTHORIZED.value());
            logger.logRequestDetails(HttpStatus.UNAUTHORIZED,currentTime,method,url,requestId,clientIp,executionTime,loginRequest,response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            loginRequest.setPassword(isUser ? loginRequest.getPassword() : "GUEST");
            loginRequest.setEmail(isUser ? loginRequest.getEmail() : "GUEST");
            String storedHashedPassword = isUser
                    ? userRepository.getUserInfoByEmail(loginRequest.getEmail()).getPasswordHash()
                    : passwordEncoder.encode("GUEST");
            if (passwordEncoder.matches(loginRequest.getPassword(), storedHashedPassword)) {
                Response responseToken = getToken(loginRequest.getEmail(), loginRequest.getPassword()).getBody();
                assert responseToken != null;
                Response response = new Response(HttpStatus.OK.value(), "User log in successfully",
                        responseToken.getTokenInfo());
                logger.logRequestDetails(HttpStatus.OK,currentTime,method,url,requestId,clientIp,executionTime,loginRequest,response);
                return ResponseEntity.status(HttpStatus.OK).body(response);

            } else {
                Response response = new Response("AUTHORIZATION_ERROR", "Password is not correct", 401);
                logger.logRequestDetails(HttpStatus.UNAUTHORIZED,currentTime,method,url,requestId,clientIp,executionTime,loginRequest,response);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            Response response = new Response("SOMETHING_WRONG", e,
                    HttpStatus.BAD_REQUEST.value());
            logger.logRequestDetails(HttpStatus.UNAUTHORIZED,currentTime,method,url,requestId,clientIp,executionTime,loginRequest,response);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
