package lk.ac.ruh.swifttray;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity @Table(name="menu_items")
public class MenuItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=100) String name;
    @Column(nullable=false,length=500) String description;
    @Column(nullable=false,length=30) String category;
    @Column(nullable=false,precision=10,scale=2) BigDecimal price;
    @Column(nullable=false) boolean available=true;
    @Column(nullable=false) boolean active=true;
    @Version Long version;
    protected MenuItem() {}
}
