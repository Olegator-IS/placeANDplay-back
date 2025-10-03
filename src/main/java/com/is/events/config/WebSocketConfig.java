package com.is.events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.is.auth.config.JwtAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    private static final String[] DEFAULT_ALLOWED_ORIGINS = {
        "http://localhost:3000",
        "http://localhost:8080",
        "https://placeandplay.uz",
        "http://placeandplay.uz",
        "http://95.46.96.94:8080",
        "https://95.46.96.94:8080"
    };

    @Value("${app.websocket.message-size-limit:128}")
    private int messageSizeLimit;

    @Value("${app.websocket.send-buffer-size-limit:512}")
    private int sendBufferSizeLimit;

    @Value("${app.websocket.send-time-limit:20000}")
    private int sendTimeLimit;

    @Value("${app.websocket.time-to-first-message:30000}")
    private int timeToFirstMessage;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public WebSocketConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-scheduler-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker for topics
        config.enableSimpleBroker("/topic", "/queue")
              .setTaskScheduler(taskScheduler())
              .setHeartbeatValue(new long[] {10000, 10000}); // 10 seconds
        
        // Set prefix for messages that are bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Set prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(DEFAULT_ALLOWED_ORIGINS)
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        logger.debug("Processing handshake request from: {}", request.getRemoteAddress());
                        
                        // Allow the handshake to proceed, we'll validate the token later
                        String token = extractToken(request);
                        if (token != null) {
                            attributes.put("token", token);
                            logger.debug("Token found in handshake request");
                        } else {
                            logger.debug("No token found in handshake request");
                        }
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                             WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            logger.error("Handshake failed: {}", exception.getMessage());
                        } else {
                            logger.debug("Handshake completed successfully");
                        }
                    }
                })
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js")
                .setDisconnectDelay(5000)
                .setHeartbeatTime(25000)
                .setSessionCookieNeeded(false)
                .setWebSocketEnabled(true)
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(messageSizeLimit * 1024)    // Convert KB to bytes
                   .setSendBufferSizeLimit(sendBufferSizeLimit * 1024)  // Convert KB to bytes
                   .setSendTimeLimit(sendTimeLimit)             // milliseconds
                   .setTimeToFirstMessage(timeToFirstMessage);  // milliseconds
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    logger.debug("Processing STOMP CONNECT command");
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String bearer = authHeader != null ? authHeader : (String) (accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("Authorization") : null);
                    String token = bearer;
                    if (bearer != null && bearer.startsWith("Bearer ")) {
                        token = bearer.substring(7);
                    }

                    if (token != null && !token.isEmpty()) {
                        try {
                            var claims = jwtAuthenticationFilter.extractClaims(token);
                            String subject = claims.getSubject();
                            if (subject != null && !subject.isEmpty()) {
                                Principal principal = () -> subject;
                                accessor.setUser(principal);
                                logger.debug("STOMP user principal set for subject: {}", subject);
                            } else {
                                logger.warn("JWT has empty subject; rejecting CONNECT");
                                throw new MessageDeliveryException("Invalid token subject");
                            }
                        } catch (Exception e) {
                            logger.error("Token validation failed: {}", e.getMessage());
                            throw new MessageDeliveryException("Invalid token");
                        }
                    } else {
                        logger.warn("No Authorization token provided for STOMP CONNECT");
                    }
                }
                return message;
            }

        });
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        converter.setObjectMapper(objectMapper);
        converter.setStrictContentTypeMatch(false);
        messageConverters.add(converter);
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return null;
    }
} 