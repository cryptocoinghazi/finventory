package com.finventory.config;

import com.finventory.security.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private static final long CORS_MAX_AGE_SECONDS = 3600L;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/v1/auth/**",
                                                "/uploads/**",
                                                "/health",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/v1/users/me", "/api/v1/users/me/password")
                                        .authenticated()
                                        .requestMatchers("/api/v1/admin/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers("/api/v1/users/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/api/v1/items/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/items/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers(HttpMethod.DELETE, "/api/v1/items/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/api/v1/offers/validate")
                                        .authenticated()
                                        .requestMatchers(HttpMethod.POST, "/api/v1/offers/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers(HttpMethod.PUT, "/api/v1/offers/**")
                                        .hasAuthority("ADMIN")
                                        .requestMatchers(HttpMethod.DELETE, "/api/v1/offers/**")
                                        .hasAuthority("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .sessionManagement(
                        sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(CORS_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
