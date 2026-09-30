package lk.ac.ruh.swifttray;

import static lk.ac.ruh.swifttray.ApiDtos.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class SwiftTrayService {
    private final UserRepository users;
    private final MenuRepository menu;
    private final OrderRepository orders;
    private final PasswordEncoder passwords;
    SwiftTrayService(UserRepository users,MenuRepository menu,OrderRepository orders,PasswordEncoder passwords) {
        this.users=users;this.menu=menu;this.orders=orders;this.passwords=passwords;
    }
    static ResponseStatusException problem(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    static String email(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    AppUser user(String email) { return users.findByEmail(email(email)).orElseThrow(()->problem(HttpStatus.UNAUTHORIZED,"Please sign in again.")); }
    static UserView view(AppUser u) { return new UserView(u.id,u.name,u.email,u.role); }
    static MenuView view(MenuItem m) { return new MenuView(m.id,m.name,m.description,m.category,m.price,m.available); }
    static OrderView view(FoodOrder o) {
        return new OrderView(o.id,"ST-"+String.format("%04d",o.id),o.student.getName(),o.status,o.total,o.createdAt,
            o.items.stream().map(i->new OrderItemView(i.menuItemId,i.name,i.quantity,i.unitPrice)).toList());
    }
    UserView register(RegisterRequest r,AppUser.Role role) {
        String mail=email(r.email());
        if(users.existsByEmail(mail)) throw problem(HttpStatus.CONFLICT,"An account already uses this email.");
        if(r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw problem(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes.");
        return view(users.save(new AppUser(r.name().strip(),mail,passwords.encode(r.password()),role)));
    }
    @Transactional(readOnly=true) List<MenuView> menu() { return menu.findByActiveTrueOrderByIdAsc().stream().map(SwiftTrayService::view).toList(); }
    MenuView saveMenu(Long id,MenuRequest r) {
        MenuItem m=id==null ? new MenuItem() : menu.lockActive(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Menu item not found."));
        m.name=r.name().strip();m.description=r.description().strip();m.category=r.category();m.price=r.price();m.available=r.available();
        return view(menu.save(m));
    }
    void removeMenu(Long id) { MenuItem m=menu.lockActive(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Menu item not found."));m.active=false; }
    OrderView place(String email,OrderRequest r) {
        AppUser student=user(email);
        if(student.role!=AppUser.Role.STUDENT) throw problem(HttpStatus.FORBIDDEN,"Only student accounts can place orders.");
        FoodOrder o=new FoodOrder();o.student=student;o.total=BigDecimal.ZERO;
        Set<Long> ids=new HashSet<>();
        for(OrderLine line:r.items().stream().sorted(Comparator.comparing(OrderLine::menuItemId)).toList()) {
            if(!ids.add(line.menuItemId())) throw problem(HttpStatus.BAD_REQUEST,"Combine duplicate menu items before ordering.");
            MenuItem m=menu.lockActive(line.menuItemId()).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"A selected menu item no longer exists."));
            if(!m.available) throw problem(HttpStatus.CONFLICT,m.name+" is currently unavailable.");
            OrderItem item=new OrderItem();item.order=o;item.menuItemId=m.id;item.name=m.name;item.quantity=line.quantity();item.unitPrice=m.price;
            o.items.add(item);o.total=o.total.add(m.price.multiply(BigDecimal.valueOf(line.quantity())));
        }
        return view(orders.saveAndFlush(o));
    }
    @Transactional(readOnly=true) List<OrderView> mine(String email) {
        return orders.findByStudentIdOrderByIdDesc(user(email).id).stream().map(SwiftTrayService::view).toList();
    }
    @Transactional(readOnly=true) List<OrderView> queue() {
        return orders.findByStatusInOrderByIdAsc(List.of(FoodOrder.Status.QUEUED,FoodOrder.Status.PREPARING,FoodOrder.Status.READY))
            .stream().map(SwiftTrayService::view).toList();
    }
    OrderView status(Long id,FoodOrder.Status next) {
        FoodOrder o=orders.lockById(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Order not found."));
        FoodOrder.Status expected=switch(o.status) {
            case QUEUED -> FoodOrder.Status.PREPARING;
            case PREPARING -> FoodOrder.Status.READY;
            case READY -> FoodOrder.Status.COLLECTED;
            default -> null;
        };
        if(next!=expected) throw problem(HttpStatus.CONFLICT,"This order has changed. Refresh the queue before updating it.");
        o.status=next;return view(o);
    }
    OrderView cancel(String email,Long id) {
        FoodOrder o=orders.lockById(id).orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Order not found."));
        if(!o.student.getId().equals(user(email).getId())) throw problem(HttpStatus.NOT_FOUND,"Order not found.");
        if(o.status!=FoodOrder.Status.QUEUED) throw problem(HttpStatus.CONFLICT,"Only queued orders can be cancelled.");
        o.status=FoodOrder.Status.CANCELLED;return view(o);
    }
}
