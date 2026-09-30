package lk.ac.ruh.swifttray;

import static lk.ac.ruh.swifttray.ApiDtos.*;
import java.util.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class ApiController {
    private final SwiftTrayService service;
    private final UserDetailsService details;
    private final PasswordEncoder passwords;
    ApiController(SwiftTrayService service,UserDetailsService details,PasswordEncoder passwords) {
        this.service=service;this.details=details;this.passwords=passwords;
    }
    @GetMapping("/health") Map<String,String> health() { return Map.of("status","UP","application","SwiftTray"); }
    @GetMapping("/auth/csrf") Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @GetMapping("/auth/me") UserView me(Authentication auth) { return SwiftTrayService.view(service.user(auth.getName())); }
    @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED)
    UserView register(@Valid @RequestBody RegisterRequest r) { return service.register(r,AppUser.Role.STUDENT); }
    @PostMapping("/auth/login") UserView login(@Valid @RequestBody LoginRequest r,HttpServletRequest req,HttpServletResponse res) {
        String email=SwiftTrayService.email(r.email());
        org.springframework.security.core.userdetails.UserDetails u;
        try { u=details.loadUserByUsername(email); }
        catch(org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            passwords.matches(r.password(),"$2a$12$eIMiTXuWVxfM37uY4JANjOcxHPx1cTt17OGkI8YDORUdaZynAEhsa");
            throw SwiftTrayService.problem(HttpStatus.UNAUTHORIZED,"Invalid email or password.");
        }
        if(!passwords.matches(r.password(),u.getPassword())) throw SwiftTrayService.problem(HttpStatus.UNAUTHORIZED,"Invalid email or password.");
        req.getSession(true);req.changeSessionId();
        SecurityContext context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(u,null,u.getAuthorities()));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,req,res);
        return SwiftTrayService.view(service.user(email));
    }
    @PostMapping("/auth/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest req) { if(req.getSession(false)!=null) req.getSession(false).invalidate();SecurityContextHolder.clearContext(); }
    @GetMapping("/menu-items") List<MenuView> menu() { return service.menu(); }
    @PostMapping("/menu-items") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    MenuView add(@Valid @RequestBody MenuRequest r) { return service.saveMenu(null,r); }
    @PutMapping("/menu-items/{id}") @PreAuthorize("hasRole('ADMIN')")
    MenuView edit(@PathVariable Long id,@Valid @RequestBody MenuRequest r) { return service.saveMenu(id,r); }
    @DeleteMapping("/menu-items/{id}") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable Long id) { service.removeMenu(id); }
    @PostMapping("/orders") @PreAuthorize("hasRole('STUDENT')") @ResponseStatus(HttpStatus.CREATED)
    OrderView place(Authentication auth,@Valid @RequestBody OrderRequest r) { return service.place(auth.getName(),r); }
    @GetMapping("/orders/mine") List<OrderView> mine(Authentication auth) { return service.mine(auth.getName()); }
    @PostMapping("/orders/{id}/cancel") @PreAuthorize("hasRole('STUDENT')")
    OrderView cancel(Authentication auth,@PathVariable Long id) { return service.cancel(auth.getName(),id); }
    @GetMapping("/staff/orders") @PreAuthorize("hasAnyRole('STAFF','ADMIN')") List<OrderView> queue() { return service.queue(); }
    @PatchMapping("/staff/orders/{id}/status") @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    OrderView status(@PathVariable Long id,@Valid @RequestBody StatusRequest r) { return service.status(id,r.status()); }
    @PostMapping("/admin/staff") @PreAuthorize("hasRole('ADMIN')") @ResponseStatus(HttpStatus.CREATED)
    UserView staff(@Valid @RequestBody RegisterRequest r) { return service.register(r,AppUser.Role.STAFF); }
}
