package com.intellidesk.cognitia.userandauth.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import com.intellidesk.cognitia.userandauth.security.JwtAuthenticationEntryPoint;
import com.intellidesk.cognitia.userandauth.security.JwtAuthenticationFilter;
import com.intellidesk.cognitia.userandauth.security.JwtTenantFilter;
import com.intellidesk.cognitia.userandauth.security.JwtTokenProvider;
import com.intellidesk.cognitia.utils.exceptionHandling.FilterExceptionHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtProvider;
    private final UserDetailsService userSDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTenantFilter jwtTenantFilter;
    private final FilterExceptionHandler filterExceptionHandler;

    public SecurityConfig(JwtTokenProvider jwtProvider, UserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, JwtTenantFilter jwtTenantFilter,
            FilterExceptionHandler filterExceptionHandler) {
        this.jwtProvider = jwtProvider;
        this.userSDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtTenantFilter = jwtTenantFilter;
        this.filterExceptionHandler = filterExceptionHandler;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4173", "http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(new AsyncRequestMatcher()).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/company").permitAll()
                .requestMatchers("/auth/**", "/api/tenants/create",  "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            // Add exception handler filter first to catch exceptions from all subsequent filters
            .addFilterBefore(filterExceptionHandler, org.springframework.web.filter.CorsFilter.class)
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userSDetailsService, jwtAuthenticationEntryPoint), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtTenantFilter, UsernamePasswordAuthenticationFilter.class);
            
            http.authenticationProvider(authenticationProvider());

        return http.build();
    }
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userSDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

