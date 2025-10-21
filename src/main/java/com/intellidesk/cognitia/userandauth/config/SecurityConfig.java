package com.intellidesk.cognitia.userandauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.intellidesk.cognitia.userandauth.security.JwtAuthenticationEntryPoint;
import com.intellidesk.cognitia.userandauth.security.JwtAuthenticationFilter;
import com.intellidesk.cognitia.userandauth.security.JwtTenantFilter;
import com.intellidesk.cognitia.userandauth.security.JwtTokenProvider;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtProvider;
    private final UserDetailsService userSDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTenantFilter jwtTenantFilter;

    public SecurityConfig(JwtTokenProvider jwtProvider, UserDetailsService userDetailsService,JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, JwtTenantFilter jwtTenantFilter) {
        this.jwtProvider = jwtProvider;
        this.userSDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtTenantFilter = jwtTenantFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/public/**", "/api/tenants/create").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, userSDetailsService), UsernamePasswordAuthenticationFilter.class)
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

