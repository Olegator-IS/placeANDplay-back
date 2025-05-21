package com.is.auth.model.user;

import com.is.auth.api.CustomAuthenticationProvider;
import com.is.auth.config.JwtAuthenticationFilter;
import com.is.auth.config.TokenSecurity;
import com.is.auth.model.dto.ActivityStatsDTO;
import com.is.auth.model.dto.ReputationDTO;
import com.is.auth.model.user.UserInfoResponse;
import com.is.auth.exception.UserAlreadyExistsException;
import com.is.auth.model.ResponseAnswers.Response;
import com.is.auth.model.enums.Language;
import com.is.auth.model.locations.CitiesDTO;
import com.is.auth.model.locations.CountriesDTO;
import com.is.auth.model.logger.Logger;
import com.is.auth.model.sports.SkillsDTO;
import com.is.auth.model.sports.SportsDTO;
import com.is.auth.repository.*;
import com.is.auth.service.EmailService;
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
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.MalformedJwtException;

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
    private final EmailService emailService;
    private final UserHobbyRepository userHobbyRepository;
    private final UserContactRepository userContactRepository;
    private final UserActivityStatsRepository userActivityStatsRepository;
    private final UserReputationRepository userReputationRepository;
    private final ObjectMapper objectMapper;

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
                      FileStorageService fileStorageService,
                      EmailService emailService,
                      UserHobbyRepository userHobbyRepository,
                      UserContactRepository userContactRepository,
                      UserActivityStatsRepository userActivityStatsRepository,
                      UserReputationRepository userReputationRepository,
                      ObjectMapper objectMapper) {
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
        this.emailService = emailService;
        this.userHobbyRepository = userHobbyRepository;
        this.userContactRepository = userContactRepository;
        this.userActivityStatsRepository = userActivityStatsRepository;
        this.userReputationRepository = userReputationRepository;
        this.objectMapper = objectMapper;
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
            emailService.sendWelcomeEmail(user.getEmail(),language);

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

    @Transactional
    public UserAdditionalInfo registerUser(Long userId, String hobbies, List<FavoriteSport> favoriteSportList,
                                         Integer currentLocationCityId, Integer currentLocationCountryId, String bio,
                                         String gender, LocalDate birthDate, String availability, String lookingFor, Boolean openToNewConnections, String contacts) {
        // Create UserAdditionalInfo
        UserAdditionalInfo userAddInfo = new UserAdditionalInfo();
        userAddInfo.setUserId(userId);
        userAddInfo.setHobbies(hobbies);
        userAddInfo.setFavoriteSports(favoriteSportList);
        userAddInfo.setCurrentLocationCityId(currentLocationCityId);
        userAddInfo.setCurrentLocationCountryId(currentLocationCountryId);
        userAddInfo.setBio(bio);
        userAddInfo.setGender(gender);
        userAddInfo.setBirthDate(birthDate);
        userAddInfo.setAvailability(availability);
        userAddInfo.setLookingFor(lookingFor);
        userAddInfo.setOpenToNewConnections(openToNewConnections);
        userAdditionalInfoRepository.save(userAddInfo);

        // Save hobbies
        if (hobbies != null && !hobbies.isEmpty()) {
            Arrays.stream(hobbies.split(","))
                  .map(String::trim)
                  .forEach(hobby -> userHobbyRepository.save(new UserHobby(userId, hobby)));
        }

        // Save contacts
        if (contacts != null) {
            try {
                UserContact userContact = objectMapper.readValue(contacts, UserContact.class);
                userContact.setUserId(userId);
                userContactRepository.save(userContact);
            } catch (Exception e) {
                log.error("Error saving user contacts for userId: {}", userId, e);
            }
        }

        // Initialize activity stats
        UserActivityStats activityStats = new UserActivityStats();
        activityStats.setUserId(userId);
        activityStats.setLastActive(LocalDateTime.now());
        userActivityStatsRepository.save(activityStats);

        // Initialize reputation
        UserReputation reputation = new UserReputation();
        reputation.setUserId(userId);
        userReputationRepository.save(reputation);

        return userAddInfo;
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
            if(accessToken.equalsIgnoreCase("SYSTEM")&&refreshToken.equalsIgnoreCase("SYSTEM")) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new Response(200, "OK", "System"));
            }
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("Access token is null or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Response(400, "TOKEN_MISSING", "Access token is required"));
            }

            String decryptedToken = TokenSecurity.decryptToken(accessToken, secretKey);
            if (decryptedToken == null) {
                log.warn("Failed to decrypt token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Response(401, "TOKEN_DECRYPTION_ERROR", "Failed to decrypt token"));
            }

            Claims claims = jwtAuthenticationFilter.extractClaims(decryptedToken);
            if (claims == null) {
                log.warn("Failed to extract claims from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new Response(401, "TOKEN_PARSE_ERROR", "Failed to parse token"));
            }

            if (claims.getSubject() != null) {
                User user = !claims.getSubject().equals("GUEST") ?
                        userRepository.getUserInfoByEmail((claims.getSubject())) : null;

                Optional<UserAdditionalInfo> userAddInfo = user != null ?
                        userAdditionalInfoRepository.findById(user.getUserId()) :
                        null;
                Map<String, Object> additionalInfo = getStringObjectMap(user, userAddInfo);
                return ResponseEntity.status(HttpStatus.OK)
                        .body(new Response(200, "OK", additionalInfo));
            } else {
                log.warn("Empty subject in token");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new Response(400, "TOKEN_VALIDATION_ERROR", "Token subject is empty"));
            }

        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "TOKEN_EXPIRED", "Token has expired"));
        } catch (SignatureException e) {
            log.warn("Invalid token signature: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "TOKEN_SIGNATURE_ERROR", "Invalid token signature"));
        } catch (MalformedJwtException e) {
            log.warn("Malformed token: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "TOKEN_MALFORMED", "Token is malformed"));
        } catch (JwtException e) {
            log.warn("Token validation error: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "TOKEN_VALIDATION_ERROR", "Token validation failed"));
        } catch (Exception e) {
            log.error("Unexpected error during token validation: {}", accessToken, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response(500, "TOKEN_ERROR", "Unexpected error during token validation: " + e.getMessage()));
        }
    }

    private Map<String, Object> getStringObjectMap(User user, Optional<UserAdditionalInfo> userAddInfo) {
        if (user == null) {
            return Map.of(
                "userId", 0,
                "firstName", "GUEST",
                "lastName", "GUEST",
                "email", "GUEST",
                "role", "GUEST",
                "profilePictureUrl", ""
            );
        }

        UserInfoResponse response = UserInfoResponse.builder()
            .userId(user.getUserId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .role("USER")
            .isEmailVerified(user.isEmailVerified())
            .profilePictureUrl("")
            .build();

        if (userAddInfo.isPresent()) {
            UserAdditionalInfo info = userAddInfo.get();
            response.setHobbies(Arrays.asList(info.getHobbies().split(",")));
            response.setFavoriteSports(info.getFavoriteSports());
            response.setBio(info.getBio());
            response.setGender(info.getGender());
            response.setBirthDate(info.getBirthDate());
            response.setCity(info.getCurrentLocationCityId() != null ? info.getCurrentLocationCityId().longValue() : null);
            response.setCountry(info.getCurrentLocationCountryId() != null ? info.getCurrentLocationCountryId().longValue() : null);
            
            // Fix availability deserialization
            try {
                if (info.getAvailability() != null) {
                    Map<String, Object> availabilityMap = objectMapper.readValue(
                        info.getAvailability(), 
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                    );
                    response.setAvailability(availabilityMap);
                }
            } catch (Exception e) {
                log.error("Error deserializing availability for user {}: {}", user.getUserId(), e.getMessage());
                response.setAvailability(null);
            }
            
            response.setLookingFor(info.getLookingFor());
            response.setOpenToNewConnections(info.getOpenToNewConnections());
            response.setDateRegistered(user.getRegistrationDate());
            response.setProfilePictureUrl(info.getProfilePictureUrl() != null ? info.getProfilePictureUrl() : "");

            // Add contacts
            UserContact contacts = userContactRepository.findByUserId(user.getUserId());
            if (contacts != null) {
                response.setContacts(Map.of(
                    "telegram", contacts.getTelegram(),
                    "instagram", contacts.getInstagram()
                ));
            }

            // Add activity stats
            UserActivityStats activityStats = userActivityStatsRepository.findByUserId(user.getUserId());
            if (activityStats != null) {
                response.setActivityStats(ActivityStatsDTO.builder()
                    .eventsPlayed(activityStats.getEventsPlayed())
                    .eventsOrganized(activityStats.getEventsOrganized())
                    .lastActive(activityStats.getLastActive())
                    .build());
            }

            // Add reputation
            UserReputation reputation = userReputationRepository.findByUserId(user.getUserId());
            if (reputation != null) {
                response.setReputation(ReputationDTO.builder()
                    .rating(reputation.getRating().doubleValue())
                    .reviewsCount(reputation.getReviewsCount())
                    .build());
            }
        }

        return objectMapper.convertValue(response, Map.class);
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

    private UserAdditionalInfo updateUserInfo(Long userId, String hobbies, List<FavoriteSport> favoriteSportList,
                                           Integer currentLocationCityId, Integer currentLocationCountryId, String bio) {
        Optional<UserAdditionalInfo> existingInfo = userAdditionalInfoRepository.findById(userId);
        UserAdditionalInfo userAddInfo;
        if (existingInfo.isPresent()) {
            userAddInfo = existingInfo.get();
            log.info("Updating existing user additional info for userId: {}", userId);
        } else {
            log.info("Creating new user additional info for userId: {}", userId);
            userAddInfo = new UserAdditionalInfo();
            userAddInfo.setUserId(userId);
        }
        if (hobbies != null) {
            userAddInfo.setHobbies(hobbies);
        }
        if (favoriteSportList != null) {
            log.info("Updating favorite sports for userId: {}. New sports list: {}", userId, favoriteSportList);
            userAddInfo.setFavoriteSports(favoriteSportList);
        }
        if (currentLocationCityId != null && currentLocationCityId > 0) {
            userAddInfo.setCurrentLocationCityId(currentLocationCityId);
        }
        if (currentLocationCountryId != null && currentLocationCountryId > 0) {
            userAddInfo.setCurrentLocationCountryId(currentLocationCountryId);
        }
        if (bio != null) {
            userAddInfo.setBio(bio);
        }
        return userAdditionalInfoRepository.save(userAddInfo);
    }

    @Transactional
    public ResponseEntity<Response> updateAddInfo(String clientIp, String url, String method, String requestId,
                                                long currentTime, long executionTime, String language,
                                                RegistrationAddInfoRequest registrationAddInfoRequest) {
        try {
            UserAdditionalInfo user = userAdditionalInfoRepository.findById(registrationAddInfoRequest.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Update basic info
            user.setHobbies(registrationAddInfoRequest.getHobbies());
            user.setFavoriteSports(registrationAddInfoRequest.getFavoriteSports());
            user.setCurrentLocationCityId(registrationAddInfoRequest.getCurrentLocationCityId());
            user.setCurrentLocationCountryId(registrationAddInfoRequest.getCurrentLocationCountryId());
            user.setBio(registrationAddInfoRequest.getBio());
            user.setGender(registrationAddInfoRequest.getGender());
            user.setBirthDate(registrationAddInfoRequest.getBirthDate());
            user.setAvailability(registrationAddInfoRequest.getAvailability());
            user.setLookingFor(registrationAddInfoRequest.getLookingFor());
            user.setOpenToNewConnections(registrationAddInfoRequest.getOpenToNewConnections());

            // Update hobbies
            userHobbyRepository.deleteByUserId(user.getUserId());
            if (registrationAddInfoRequest.getHobbies() != null && !registrationAddInfoRequest.getHobbies().isEmpty()) {
                String[] hobbies = registrationAddInfoRequest.getHobbies().split(",");
                for (String hobby : hobbies) {
                    UserHobby userHobby = new UserHobby();
                    userHobby.setUserId(user.getUserId());
                    userHobby.setHobby(hobby.trim());
                    userHobbyRepository.save(userHobby);
                }
            }

            // Update contacts
            if (registrationAddInfoRequest.getContacts() != null) {
                try {
                    UserContact userContact = objectMapper.readValue(registrationAddInfoRequest.getContacts(), UserContact.class);
                    userContact.setUserId(user.getUserId());
                    userContactRepository.save(userContact);
                } catch (Exception e) {
                    log.error("Error updating contacts for user {}: {}", user.getUserId(), e.getMessage());
                    throw new RuntimeException("Invalid contacts format: " + e.getMessage());
                }
            }

            // Save the updated user info
            userAdditionalInfoRepository.save(user);

            Response response = new Response(HttpStatus.OK.value(), "USER_UPDATED_SUCCESSFULLY", user.getUserId());
            logger.logRequestDetails(HttpStatus.OK, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationAddInfoRequest, response);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error updating user info: {}", e.getMessage(), e);
            Response response = new Response(HttpStatus.BAD_REQUEST.value(), "UPDATE_ERROR",
                    "Error during update: " + e.getMessage());
            logger.logRequestDetails(HttpStatus.BAD_REQUEST, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationAddInfoRequest, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Unexpected error updating user info: {}", e.getMessage(), e);
            Response response = new Response(HttpStatus.INTERNAL_SERVER_ERROR.value(), "UPDATE_ERROR",
                    "Unexpected error during update: " + e.getMessage());
            logger.logRequestDetails(HttpStatus.INTERNAL_SERVER_ERROR, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationAddInfoRequest, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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
                Response responseToken =    getToken(loginRequest.getEmail(), loginRequest.getPassword()).getBody();
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

    @Transactional
    public ResponseEntity<Response> uploadProfilePicture(MultipartFile file, Long userId) {
        try {
            log.info("Полученный userID при смене аватара {}", userId);
            Optional<UserAdditionalInfo> userInfo = userAdditionalInfoRepository.findById(userId);
            if (!userInfo.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new Response(404, "USER_NOT_FOUND", "User not found"));
            }
            String fileUrl = fileStorageService.uploadFile(file, "profile-pictures");
            
            // Обновляем URL профильной картинки в базе данных
            UserAdditionalInfo userAdditionalInfo = userInfo.get();
            userAdditionalInfo.setProfilePictureUrl(fileUrl);
            userAdditionalInfoRepository.save(userAdditionalInfo);
            
            return ResponseEntity.ok(new Response(200, "PROFILE_PICTURE_UPDATED", fileUrl));
        } catch (Exception e) {
            log.error("Error uploading profile picture for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Response(500, "UPLOAD_ERROR", "Error uploading file"));
        }
    }

    @Transactional
    public ResponseEntity<Response> registrationAddInfo(String clientIp, String url, String method, String requestId,
                                                      long currentTime, long executionTime, String language,
                                                      RegistrationAddInfoRequest registrationAddInfoRequest) {
        try {
            log.info("Starting registration of additional info for user ID: {}", registrationAddInfoRequest.getUserId());
            
            // Check if user exists
            if (!userRepository.existsById(registrationAddInfoRequest.getUserId())) {
                log.error("User not found with ID: {}", registrationAddInfoRequest.getUserId());
                Response response = new Response(HttpStatus.NOT_FOUND.value(), "USER_NOT_FOUND", 
                    "User with ID " + registrationAddInfoRequest.getUserId() + " not found");
                logger.logRequestDetails(HttpStatus.NOT_FOUND, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationAddInfoRequest, response);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // Create UserAdditionalInfo
            final UserAdditionalInfo userAddInfo = new UserAdditionalInfo();
            userAddInfo.setUserId(registrationAddInfoRequest.getUserId());
            userAddInfo.setHobbies(registrationAddInfoRequest.getHobbies());
            userAddInfo.setFavoriteSports(registrationAddInfoRequest.getFavoriteSports());
            userAddInfo.setCurrentLocationCityId(registrationAddInfoRequest.getCurrentLocationCityId());
            userAddInfo.setCurrentLocationCountryId(registrationAddInfoRequest.getCurrentLocationCountryId());
            userAddInfo.setBio(registrationAddInfoRequest.getBio());
            userAddInfo.setGender(registrationAddInfoRequest.getGender());
            userAddInfo.setBirthDate(registrationAddInfoRequest.getBirthDate());
            userAddInfo.setAvailability(registrationAddInfoRequest.getAvailability());
            userAddInfo.setLookingFor(registrationAddInfoRequest.getLookingFor());
            userAddInfo.setOpenToNewConnections(registrationAddInfoRequest.getOpenToNewConnections());
            userAddInfo.setLastLogin(LocalDateTime.now());
            
            // Save UserAdditionalInfo
            userAdditionalInfoRepository.save(userAddInfo);
            log.info("Saved UserAdditionalInfo for user ID: {}", userAddInfo.getUserId());

            // Save hobbies
            if (registrationAddInfoRequest.getHobbies() != null && !registrationAddInfoRequest.getHobbies().isEmpty()) {
                Arrays.stream(registrationAddInfoRequest.getHobbies().split(","))
                      .map(String::trim)
                      .forEach(hobby -> {
                          UserHobby userHobby = new UserHobby(userAddInfo.getUserId(), hobby);
                          userHobbyRepository.save(userHobby);
                          log.debug("Saved hobby: {} for user ID: {}", hobby, userAddInfo.getUserId());
                      });
            }

            // Save contacts
            if (registrationAddInfoRequest.getContacts() != null) {
                try {
                    UserContact userContact = objectMapper.readValue(registrationAddInfoRequest.getContacts(), UserContact.class);
                    userContact.setUserId(userAddInfo.getUserId());
                    userContactRepository.save(userContact);
                    log.info("Saved contacts for user ID: {}", userAddInfo.getUserId());
                } catch (Exception e) {
                    log.error("Error saving user contacts for userId: {}", userAddInfo.getUserId(), e);
                    throw new RuntimeException("Invalid contacts format: " + e.getMessage());
                }
            }

            // Initialize activity stats
            UserActivityStats activityStats = new UserActivityStats();
            activityStats.setUserId(userAddInfo.getUserId());
            activityStats.setLastActive(LocalDateTime.now());
            userActivityStatsRepository.save(activityStats);
            log.info("Initialized activity stats for user ID: {}", userAddInfo.getUserId());

            // Initialize reputation
            UserReputation reputation = new UserReputation();
            reputation.setUserId(userAddInfo.getUserId());
            userReputationRepository.save(reputation);
            log.info("Initialized reputation for user ID: {}", userAddInfo.getUserId());

            Response response = new Response(HttpStatus.CREATED.value(), "USER_CREATED_SUCCESSFULLY", userAddInfo.getUserId());
            logger.logRequestDetails(HttpStatus.CREATED, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationAddInfoRequest, response);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error during registration of additional info: {}", e.getMessage(), e);
            Response response = new Response(HttpStatus.BAD_REQUEST.value(), "REGISTRATION_ERROR",
                    "Error during registration: " + e.getMessage());
            logger.logRequestDetails(HttpStatus.BAD_REQUEST, currentTime, method, url, requestId, clientIp, executionTime,
                    registrationAddInfoRequest, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // Getter for userRepository
    public UserRepository getUserRepository() {
        return userRepository;
    }

    public ResponseEntity<?> checkEmailVerificationStatus(String email, String language) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> messages = Map.of(
            "ru", "Email не подтвержден. Пожалуйста, проверьте вашу почту или запросите новый код подтверждения.",
            "en", "Email is not verified. Please check your inbox or request a new verification code.",
            "uz", "Email tasdiqlanmagan. Iltimos, pochtangizni tekshiring yoki yangi tasdiqlash kodini so'rang."
        );
        
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                response.put("status", "error");
                response.put("message", "User not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            User user = userOptional.get();
            response.put("status", "success");
            response.put("isVerified", user.isEmailVerified());
            
            if (!user.isEmailVerified()) {
                response.put("message", messages.getOrDefault(language, messages.get("ru")));
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking email verification status for email: {}", email, e);
            response.put("status", "error");
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}



