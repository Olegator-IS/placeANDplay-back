package com.is.auth.model.user;

import com.is.auth.api.CustomAuthenticationProvider;
import com.is.auth.config.JwtAuthenticationFilter;
import com.is.auth.config.TokenSecurity;
import com.is.auth.exception.UserAlreadyExistsException;
import com.is.auth.model.ResponseAnswers.Response;
import com.is.auth.model.enums.Language;
import com.is.auth.model.locations.CitiesDTO;
import com.is.auth.model.locations.CountriesDTO;
import com.is.auth.model.logger.Logger;
import com.is.auth.model.sports.SkillsDTO;
import com.is.auth.model.sports.SportsDTO;
import com.is.auth.repository.*;
import com.is.auth.service.RequestLogger;
import com.is.auth.service.FileStorageService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class UserService {

    private final SecretKey secretKey;
    private final RequestLogger requestLogger;
    private final ListOfSportsRepository listOfSportsRepository;
    private final UserRepository userRepository;
    private final UserAdditionalInfoRepository userAdditionalInfoRepository;
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ListOfSkillsRepository listOfSkillsRepository;
    private final ListOfCitiesRepository listOfCitiesRepository;
    private final ListOfCountriesRepository listOfCountriesRepository;
    private final FileStorageService fileStorageService;

    @Autowired
    private Logger logger;

    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;

    public UserService(SecretKey secretKey,
                      JwtAuthenticationFilter jwtAuthenticationFilter,
                      RequestLogger requestLogger,
                      ListOfSportsRepository listOfSportsRepository,
                      UserRepository userRepository,
                      UserAdditionalInfoRepository userAdditionalInfoRepository,
                      CustomAuthenticationProvider customAuthenticationProvider,
                      BCryptPasswordEncoder passwordEncoder,
                      ListOfSkillsRepository listOfSkillsRepository,
                      ListOfCitiesRepository listOfCitiesRepository,
                      ListOfCountriesRepository listOfCountriesRepository,
                      FileStorageService fileStorageService) {
        this.secretKey = secretKey;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.requestLogger = requestLogger;
        this.listOfSportsRepository = listOfSportsRepository;
        this.userRepository = userRepository;
        this.userAdditionalInfoRepository = userAdditionalInfoRepository;
        this.customAuthenticationProvider = customAuthenticationProvider;
        this.passwordEncoder = passwordEncoder;
        this.listOfSkillsRepository = listOfSkillsRepository;
        this.listOfCitiesRepository = listOfCitiesRepository;
        this.listOfCountriesRepository = listOfCountriesRepository;
        this.fileStorageService = fileStorageService;
    }

    private User createNewUser(String email, String password, String firstName, String lastName) {
        return User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .emailVerified(false)
                .registrationDate(LocalDateTime.now())
                .build();
    }

    private Map<String, String> generateTokens(String email, Authentication authentication) throws Exception {
        String accessToken = jwtAuthenticationFilter.generateToken(email, authentication);
        String refreshToken = jwtAuthenticationFilter.generateRefreshToken(email, authentication);
        
        if (accessToken.equals("Error")) {
            throw new RuntimeException("Error generating token");
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", TokenSecurity.encryptToken(accessToken, secretKey));
        tokens.put("refreshToken", TokenSecurity.encryptToken(refreshToken, secretKey));
        return tokens;
    }

    public ResponseEntity<Response> registrationRequest(String clientIp, String url, String method, String requestId,
                                                      long currentTime, long executionTime, String language,
                                                      RegistrationRequest registrationRequest) {
        try {
            if (userRepository.existsByEmail(registrationRequest.getEmail())) {
                throw new UserAlreadyExistsException(registrationRequest.getEmail());
            }

            User user = createNewUser(
                    registrationRequest.getEmail(),
                    registrationRequest.getPassword(),
                    registrationRequest.getFirstName(),
                    registrationRequest.getLastName()
            );
            user = userRepository.save(user);

            Response response = new Response(HttpStatus.CREATED.value(), "USER_CREATED_SUCCESSFULLY", user.getUserId());
            requestLogger.logRequest(HttpStatus.CREATED, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationRequest, response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserAlreadyExistsException e) {
            Response response = new Response("EMAIL_IS_ALREADY_EXIST", "Email already in use",
                    HttpStatus.CONFLICT.value());
            requestLogger.logRequest(HttpStatus.CONFLICT, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationRequest, response);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("Error during user registration", e);
            Response response = new Response(HttpStatus.BAD_REQUEST.value(), "Registration failed",
                    e.getMessage());
            requestLogger.logRequest(HttpStatus.BAD_REQUEST, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationRequest, response);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @Cacheable(value = "sports", key = "#language")
    public List<SportsDTO> getSportsByLanguage(String language) {
        String columnName = Language.getColumnNameByCode(language);
        return listOfSportsRepository.findByLanguage(columnName);
    }

    @Cacheable(value = "sports", key = "#language")
    public String getAttrModelBySport(int sportId,String language) {
        return listOfSportsRepository.findBySportId(sportId);
    }

    @Cacheable(value = "skills", key = "#language")
    public List<SkillsDTO> getListOfSkills(String language) {
        String columnName = Language.getColumnNameByCode(language);
        return listOfSkillsRepository.findByLanguage(columnName);
    }

    @Cacheable(value = "cities", key = "#language + '-' + #countryCode")
    public List<CitiesDTO> getListOfCities(String language, Integer countryCode) {
        String columnName = Language.getColumnNameByCode(language);
        return listOfCitiesRepository.findByLanguage(columnName, countryCode);
    }

    @Cacheable(value = "countries", key = "#language")
    public List<CountriesDTO> getListOfCountries(String language) {
        String columnName = Language.getColumnNameByCode(language);
        return listOfCountriesRepository.findByLanguage(columnName);
    }

    public UserAdditionalInfo  registerUser(long userId, String hobbies, List<FavoriteSport> favoriteSportList,
                                           int currentLocationCityId, int currentLocationCountryId, String bio, String profilePictureUrl) {




        UserAdditionalInfo userAddInfo = new UserAdditionalInfo();
        userAddInfo.setUserId(userId);
        userAddInfo.setHobbies(hobbies);
        userAddInfo.setFavoriteSports(FavoriteSport.toJson(favoriteSportList));
        userAddInfo.setCurrentLocationCityId(currentLocationCityId);
        userAddInfo.setCurrentLocationCountryId(currentLocationCountryId);
        userAddInfo.setBio(bio);
        userAddInfo.setProfilePictureUrl(profilePictureUrl);
        System.out.println(userAddInfo.getFavoriteSports());

        return userAdditionalInfoRepository.save(userAddInfo);
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

    public ResponseEntity<Response> validateTokenAndGetSubject(String accessToken, String refreshToken,String language) {
        try {
            String decryptedToken = TokenSecurity.decryptToken(accessToken, secretKey);
            Claims claims = jwtAuthenticationFilter.extractClaims(decryptedToken);

            if (claims.getSubject() != null) {

                User user = !claims.getSubject().equals("GUEST")?
                        userRepository.getUserInfoByEmail((claims.getSubject())):null;

                Optional<UserAdditionalInfo> userAddInfo = user != null ?
                        userAdditionalInfoRepository.findById(user.getUserId()) :
                        null;
                Map<String, Object> additionalInfo = getStringObjectMap(user, userAddInfo);
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

    private static Map<String, Object> getStringObjectMap(User user, Optional<UserAdditionalInfo> userAddInfo) {
        Map<String, Object> additionalInfo = new HashMap<>();

        additionalInfo.put("userId", user !=null? user.getUserId():0);
        additionalInfo.put("firstName", user !=null? user.getFirstName():"GUEST");
        additionalInfo.put("lastName", user !=null? user.getLastName():"GUEST");
        additionalInfo.put("email", user !=null? user.getEmail():"GUEST");
        additionalInfo.put("role", user !=null?"USER":"GUEST");
        additionalInfo.put("isEmailVerified", user != null && user.isEmailVerified());
        additionalInfo.put("city", user != null ? userAddInfo.map(info -> info.getCurrentLocationCityId()).orElse(0)
                : 0);
        additionalInfo.put("hobbies", user != null ? userAddInfo.map(info -> info.getHobbies()).orElse("No hobbies")
                : "No hobbies");
        additionalInfo.put("bio", user != null ? userAddInfo.map(info -> info.getBio()).orElse("No bio")
                : "No bio");
        additionalInfo.put("profile_picture_url", user != null ? userAddInfo.map(info -> info.getProfilePictureUrl()).orElse("")
                : "");
        additionalInfo.put("favoriteSports", user != null ? userAddInfo.map(info -> {
            Object sports = info.getFavoriteSportObjects();
            return sports != null ? sports : new ArrayList<>();
        }).orElse(new ArrayList<>()) : new ArrayList<>());
        return additionalInfo;
    }

    public ResponseEntity<Response> refreshToken(String refreshToken,String language) {
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

    public ResponseEntity<Response> registrationAddInfo(String clientIp,String url,String method,String requestId,
                                                        long currentTime,long executionTime,String language,
                                                        RegistrationAddInfoRequest registrationAddInfoRequest) {
        try {

            UserAdditionalInfo user = registerUser(registrationAddInfoRequest.getUserId(),
                    registrationAddInfoRequest.getHobbies(),registrationAddInfoRequest.getFavoriteSports(),
                    registrationAddInfoRequest.getCurrentLocationCityId(),registrationAddInfoRequest.getCurrentLocationCountryId(),
                    registrationAddInfoRequest.getBio(),registrationAddInfoRequest.getProfilePictureUrl());

            Response response = new Response(HttpStatus.CREATED.value(), "USER_CREATED_SUCCESSFULLY", user.getUserId());
            logger.logRequestDetails(HttpStatus.CREATED,currentTime,method,url,requestId,clientIp,executionTime,registrationAddInfoRequest,response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            Response response = new Response(HttpStatus.BAD_REQUEST.value(), "SOMETHING_WRONG",
                    "Error during processing, try again");
            logger.logRequestDetails(HttpStatus.BAD_REQUEST,currentTime,method,url,requestId,clientIp,executionTime,registrationAddInfoRequest,e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    public ResponseEntity<Response> loginRequest(String clientIp,String url,String method,String requestId,
                                                 long currentTime,long executionTime,
                                                 String language,LoginRequest loginRequest,
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

    public ResponseEntity<Response> uploadProfilePicture(MultipartFile file, Long userId) {
        try {
            // Проверяем существование пользователя'
            log.info("Полученный userID при смене аватара {}",userId);
            Optional<UserAdditionalInfo> userInfo = userAdditionalInfoRepository.findById(userId);
            if (!userInfo.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Response(404, "USER_NOT_FOUND", "User not found"));
            }

            // Загружаем файл в локальное хранилище
            String fileUrl = fileStorageService.uploadFile(file, "profile-pictures");
            
            // Обновляем URL в базе данных
            UserAdditionalInfo info = userInfo.get();
            info.setProfilePictureUrl(fileUrl);
            userAdditionalInfoRepository.save(info);
            
            return ResponseEntity.ok(new Response(200, "PROFILE_PICTURE_UPDATED", fileUrl));
        } catch (Exception e) {
            log.error("Error uploading profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Response(500, "UPLOAD_ERROR", "Error uploading file"));
        }
    }
}



