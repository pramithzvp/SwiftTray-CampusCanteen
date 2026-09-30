package lk.ac.ruh.swifttray;

import jakarta.persistence.*;

@Entity
@Table(name="users")
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false, length=80) String name;
    @Column(nullable=false, unique=true, length=180) String email;
    @Column(nullable=false) String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false) Role role;
    public enum Role { STUDENT, STAFF, ADMIN }
    protected AppUser() {}
    public Long getId() { return id; }
    public String getName() { return name; }
    AppUser(String name, String email, String hash, Role role) {
        this.name=name; this.email=email; this.passwordHash=hash; this.role=role;
    }
}
