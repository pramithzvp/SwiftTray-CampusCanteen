package lk.ac.ruh.swifttray;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {
    private final SwiftTrayService service;
    private final UserRepository users;
    @Value("${swifttray.admin.email:}") String email;
    @Value("${swifttray.admin.password:}") String password;
    AdminBootstrap(SwiftTrayService service,UserRepository users) {this.service=service;this.users=users;}
    @Override public void run(String... args) {
        if(email.isBlank() && password.isBlank()) return;
        if(email.isBlank() || !email.contains("@") || password.length()<12)
            throw new IllegalStateException("Set a valid SWIFTTRAY_ADMIN_EMAIL and SWIFTTRAY_ADMIN_PASSWORD of at least 12 characters.");
        if(!users.existsByEmail(SwiftTrayService.email(email)))
            service.register(new ApiDtos.RegisterRequest("Canteen administrator",email,password),AppUser.Role.ADMIN);
    }
}
