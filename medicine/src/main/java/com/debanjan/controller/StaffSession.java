package com.debanjan.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.bind.annotation.*;

@RestController
public class StaffSession {
    @Bean
    SecurityFilterChain staffAccess(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .formLogin(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        (request, response, failure) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")))
                .build();
    }

    @GetMapping("/api/session")
    public CsrfToken session(CsrfToken token) {
        return token;
    }
}
