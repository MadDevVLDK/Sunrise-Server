package com.sunrise.web.websocket.config;

import com.sunrise.web.jwt.JwtHandshakeInterceptor;
import com.sunrise.web.websocket.annotation.WsUserIdArgumentResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WsConfig implements WebSocketMessageBrokerConfigurer {

    private final WsSubscriptionInterceptor subscriptionInterceptor;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final WsHandshakeHandler wsHandshakeHandler;
    private final WsUserIdArgumentResolver wsUserIdArgumentResolver;

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(subscriptionInterceptor);
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(wsHandshakeHandler)
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:5173", "http://localhost:8080")
                .withSockJS()
                .setHttpMessageCacheSize(4096)
                .setHeartbeatTime(25000);
    }

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(160 * 1024); // 160kb
        registration.setSendTimeLimit(20 * 1000); // 20 seconds
        registration.setSendBufferSizeLimit(512 * 1024); // 512kb
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(wsUserIdArgumentResolver);
    }
}
