package com.qzh.symphony.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;

import java.util.Arrays;

//给Spring Security层用的
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                //禁用CSRF,用于放行Dify的请求
                .csrf(csrf -> csrf.disable())
                //启用跨域支持
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //设置为无状态 session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //放行白名单
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/ai/callback",         // Dify 回调
                                "/ws/ai-chat/**",           // WebSocket
                                "/api/ai/listPersonalAis/**",
                                "/api/ai/createPersonalAi",
                                "/api/ai/delete/**",
                                "/api/ai/updateAi",
                                "/api/chat/ai/stream",
                                "/symphony",
                                "/api/friends/request",
                                "/api/friends/request/handle",
                                "/api/friends/requests/pending",
                                "/api/chat/private/list",
                                "/api/chat/private/*/messages",
                                "/api/chat/private/send",
                                "/api/chat/public/messages",
                                "/api/chat/private/ai/summarize",
                                "/api/mistakes/list",
                                "/api/mistakes/add",
                                "/api/mistakes/*",
                                "/api/mistakes/analysis",
                                "/api/mistakes/practice",
                                "/ws/collab/**",
                                "/api/collab/**",
                                "/api/report/**",
                                "/api/user/**",
                                "/api/mistakes/suggest",
                                "/api/garden/*"
                        ).permitAll()
                        //其他所有请求需要JWT验证
                        .anyRequest().authenticated()
                )
                //将JWT过滤器放在账号密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    //跨域配置,允许Dify和前端机器访问
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}