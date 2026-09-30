package lk.ac.ruh.swifttray;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwords() { return new BCryptPasswordEncoder(12); }
    @Bean UserDetailsService userDetails(UserRepository users) {
        return email->users.findByEmail(SwiftTrayService.email(email))
            .map(u->User.withUsername(u.email).password(u.passwordHash).roles(u.role.name()).build())
            .orElseThrow(()->new UsernameNotFoundException("Invalid email or password."));
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a->a
            .requestMatchers("/api/health","/api/auth/csrf","/api/auth/register","/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.GET,"/api/menu-items").permitAll()
            .anyRequest().authenticated())
            .csrf(c->c.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .requestCache(c->c.disable()).formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .exceptionHandling(e->e
                .authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Please sign in to continue.\"}");})
                .accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"message\":\"Access denied or session expired. Refresh and try again.\"}");}));
        return http.build();
    }
}
