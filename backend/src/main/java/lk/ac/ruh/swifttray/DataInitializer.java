package lk.ac.ruh.swifttray;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
@Order(10)
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SwiftTrayService service;
    private final UserRepository users;
    private final MenuRepository menu;
    private final OrderRepository orders;

    public DataInitializer(SwiftTrayService service, UserRepository users, MenuRepository menu, OrderRepository orders) {
        this.service = service;
        this.users = users;
        this.menu = menu;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAccounts();
        seedMenu();
        seedSampleOrders();
    }

    private void seedAccounts() {
        if (!users.existsByEmail("admin@swifttray.lk")) {
            service.register(new ApiDtos.RegisterRequest("Canteen Administrator", "admin@swifttray.lk", "SwiftTrayAdmin2026!"), AppUser.Role.ADMIN);
            log.info("Seeded administrator account: admin@swifttray.lk");
        }
        if (!users.existsByEmail("staff@swifttray.lk")) {
            service.register(new ApiDtos.RegisterRequest("Canteen Staff", "staff@swifttray.lk", "SwiftTrayStaff2026!"), AppUser.Role.STAFF);
            log.info("Seeded staff account: staff@swifttray.lk");
        }
        if (!users.existsByEmail("student@swifttray.lk")) {
            service.register(new ApiDtos.RegisterRequest("Kamal Perera", "student@swifttray.lk", "SwiftTrayStudent2026!"), AppUser.Role.STUDENT);
            log.info("Seeded student account: student@swifttray.lk");
        }
    }

    private void seedMenu() {
        if (menu.count() > 0) return;

        createItem("Sri Lankan Rice & Curry (Chicken)", "Steaming samba rice served with dhal curry, pol sambol, fried sprats, and rich chicken curry.", "Meals", "380.00");
        createItem("Sri Lankan Rice & Curry (Fish)", "Steamed white rice served with spicy ambul thiyal fish curry, tempered dhal, and gotukola sambol.", "Meals", "350.00");
        createItem("Sri Lankan Rice & Curry (Vegetarian)", "Wholesome red rice accompanied by creamy dhal curry, tempered devilled potato, and sweet pumpkin.", "Meals", "280.00");
        createItem("Chicken Kottu Roti", "Chopped godamba roti stir-fried on the hot griddle with spiced chicken curry, egg, and fresh leeks.", "Meals", "450.00");
        createItem("Egg & Vegetable Fried Rice", "Fragrant wok-tossed basmati rice with farm fresh eggs, diced carrots, spring onion, and chili paste.", "Meals", "380.00");
        createItem("Chicken Dum Biryani", "Aromatic basmati rice layered with tender spiced chicken, boiled egg, mint, fried onions, and cucumber raita.", "Meals", "520.00");

        createItem("Fish Bun (Malu Paan)", "Classic Sri Lankan triangular baked bread stuffed with spiced canned fish, potatoes, and black pepper.", "Snacks", "100.00");
        createItem("Chicken Spring Roll", "Crispy golden fried pancake roll filled with seasoned minced chicken, shredded cabbage, and carrots.", "Snacks", "120.00");
        createItem("Vegetable Roti", "Flaky flatbread folded with a spicy curried potato and vegetable stuffing.", "Snacks", "80.00");
        createItem("Egg Pastry", "Layers of crisp puff pastry enclosing half a boiled egg and caramelized onion relish.", "Snacks", "110.00");
        createItem("Ulundu Vadai", "Traditional crispy, savoury lentil donut fritter served with fresh coconut chutney.", "Snacks", "70.00");

        createItem("Ceylon Milk Tea", "Freshly brewed rich Ceylon black tea with sweetened condensed milk and crushed cardamom.", "Drinks", "80.00");
        createItem("Canteen Iced Coffee", "Chilled dark roast coffee blended with fresh milk, vanilla bean extract, and crushed ice.", "Drinks", "150.00");
        createItem("Fresh Lime Juice", "Freshly squeezed green lime juice served chilled with crushed mint leaves and cane sugar syrup.", "Drinks", "120.00");
        createItem("Faluda Shake", "Sweet rose syrup milk shake with basil seeds (kasa kasa), grass jelly, and vanilla ice cream.", "Drinks", "180.00");
        createItem("Chilled Mineral Water (500ml)", "Pure bottled mineral spring water, chilled.", "Drinks", "70.00");

        log.info("Seeded 16 canteen menu items across Meals, Snacks, and Drinks.");
    }

    private void createItem(String name, String desc, String cat, String price) {
        service.saveMenu(null, new ApiDtos.MenuRequest(name, desc, cat, new BigDecimal(price), true));
    }

    private void seedSampleOrders() {
        if (orders.count() > 0) return;

        List<MenuItem> items = menu.findByActiveTrueOrderByIdAsc();
        if (items.size() < 4) return;

        try {
            // Order 1: Queued order for student
            ApiDtos.OrderView o1 = service.place("student@swifttray.lk", new ApiDtos.OrderRequest(List.of(
                new ApiDtos.OrderLine(items.get(0).id, 1),
                new ApiDtos.OrderLine(items.get(11).id, 1)
            )));
            log.info("Created sample queued order: {}", o1.token());

            // Order 2: Preparing order for student
            ApiDtos.OrderView o2 = service.place("student@swifttray.lk", new ApiDtos.OrderRequest(List.of(
                new ApiDtos.OrderLine(items.get(3).id, 1),
                new ApiDtos.OrderLine(items.get(12).id, 1)
            )));
            service.status(o2.id(), FoodOrder.Status.PREPARING);
            log.info("Created sample preparing order: {}", o2.token());

            // Order 3: Ready order for student
            ApiDtos.OrderView o3 = service.place("student@swifttray.lk", new ApiDtos.OrderRequest(List.of(
                new ApiDtos.OrderLine(items.get(6).id, 2),
                new ApiDtos.OrderLine(items.get(13).id, 1)
            )));
            service.status(o3.id(), FoodOrder.Status.PREPARING);
            service.status(o3.id(), FoodOrder.Status.READY);
            log.info("Created sample ready order: {}", o3.token());
        } catch (Exception e) {
            log.warn("Sample orders could not be placed during init: {}", e.getMessage());
        }
    }
}
