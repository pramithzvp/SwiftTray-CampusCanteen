package lk.ac.ruh.swifttray;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity @Table(name="order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) FoodOrder order;
    @Column(nullable=false) Long menuItemId;
    @Column(nullable=false,length=100) String name;
    @Column(nullable=false) int quantity;
    @Column(nullable=false,precision=10,scale=2) BigDecimal unitPrice;
    protected OrderItem() {}
}
