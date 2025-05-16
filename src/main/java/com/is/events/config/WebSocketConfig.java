package com.is.events.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.context.annotation.Bean;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        // Get token from request headers
                        String token = request.getHeaders().getFirst("Authorization");
                        if (token != null && token.startsWith("Bearer ")) {
                            token = token.substring(7);
                        }
                        attributes.put("token", token);
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                             WebSocketHandler wsHandler, Exception exception) {
                        // No action needed after handshake
                    }
                })
                .withSockJS();
        
        // Добавляем эндпоинт без SockJS для прямого WebSocket подключения
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                 WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        // Get token from request headers
                        String token = request.getHeaders().getFirst("Authorization");
                        if (token != null && token.startsWith("Bearer ")) {
                            token = token.substring(7);
                        }
                        attributes.put("token", token);
                        return true;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                             WebSocketHandler wsHandler, Exception exception) {
                        // No action needed after handshake
                    }
                });
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(128 * 1024)    // 128KB
                   .setSendBufferSizeLimit(512 * 1024)  // 512KB
                   .setSendTimeLimit(20000)             // 20 seconds
                   .setTimeToFirstMessage(30000);       // 30 seconds
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
} 