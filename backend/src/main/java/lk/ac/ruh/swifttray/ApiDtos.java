package lk.ac.ruh.swifttray;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class ApiDtos {
    private ApiDtos() {}
    public record RegisterRequest(@NotBlank @Size(max=80) String name,
        @NotBlank @Email @Size(max=180) String email, @NotBlank @Size(min=12,max=72) String password) {}
    public record LoginRequest(@NotBlank @Email @Size(max=180) String email, @NotBlank @Size(max=72) String password) {}
    public record UserView(Long id,String name,String email,AppUser.Role role) {}
    public record MenuRequest(@NotBlank @Size(max=100) String name,
        @NotBlank @Size(max=500) String description,
        @Pattern(regexp="Meals|Snacks|Drinks",message="must be Meals, Snacks or Drinks") @NotNull String category,
        @NotNull @DecimalMin("1.00") @DecimalMax("999999.99") @Digits(integer=6,fraction=2) BigDecimal price,
        boolean available) {}
    public record MenuView(Long id,String name,String description,String category,BigDecimal price,boolean available) {}
    public record OrderLine(@NotNull Long menuItemId,@Min(1) @Max(20) int quantity) {}
    public record OrderRequest(@NotEmpty @Size(max=20) List<@Valid OrderLine> items) {}
    public record StatusRequest(@NotNull FoodOrder.Status status) {}
    public record OrderItemView(Long menuItemId,String name,int quantity,BigDecimal unitPrice) {}
    public record OrderView(Long id,String token,String studentName,FoodOrder.Status status,
        BigDecimal total,Instant createdAt,List<OrderItemView> items) {}
}
