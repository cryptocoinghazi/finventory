package com.finventory.config;

import com.finventory.model.Role;
import com.finventory.model.User;
import com.finventory.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig implements WebMvcConfigurer {

    private static final String SEEDED_ADMIN_PASSWORD_BCRYPT =
            "$2a$10$wPHxwfsfTnOJAdgYcerBt.utdAvC24B/DWfuXfzKBSDHO0etB1ica";

    private final UserRepository userRepository;

    @Value("${application.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${application.security.bootstrap-admin.enabled:true}")
    private boolean bootstrapAdminEnabled;

    @Value("${application.security.bootstrap-admin.username:admin}")
    private String bootstrapAdminUsername;

    @Value("${application.security.bootstrap-admin.email:admin@finventory.com}")
    private String bootstrapAdminEmail;

    @Value("${application.security.bootstrap-admin.password:admin123}")
    private String bootstrapAdminPassword;

    @Bean
    public UserDetailsService userDetailsService() {
        return username ->
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public ApplicationRunner ensureUploadsDirectoryExists() {
        return args -> Files.createDirectories(getUploadsPath());
    }

    @Bean
    public ApplicationRunner ensureBootstrapAdminUser(PasswordEncoder passwordEncoder) {
        return args -> {
            if (!bootstrapAdminEnabled) {
                return;
            }

            userRepository
                    .findByUsername(bootstrapAdminUsername)
                    .ifPresentOrElse(
                            user -> {
                                if (SEEDED_ADMIN_PASSWORD_BCRYPT.equals(user.getPassword())) {
                                    user.setPassword(passwordEncoder.encode(bootstrapAdminPassword));
                                    userRepository.save(user);
                                }
                            },
                            () -> {
                                User user =
                                        User.builder()
                                                .username(bootstrapAdminUsername)
                                                .email(bootstrapAdminEmail)
                                                .password(passwordEncoder.encode(bootstrapAdminPassword))
                                                .role(Role.ADMIN)
                                                .build();
                                userRepository.save(user);
                            });
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Executor migrationPipelineExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = getUploadsPath().toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    private Path getUploadsPath() {
        return Paths.get(uploadsDir).toAbsolutePath().normalize();
    }
}
