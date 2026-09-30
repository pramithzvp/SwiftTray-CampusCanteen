package lk.ac.ruh.swifttray;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import jakarta.persistence.*;

@Entity @Table(name="orders",indexes={@Index(name="idx_order_status_id",columnList="status,id")})
public class FoodOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) AppUser student;
    @Enumerated(EnumType.STRING) @Column(nullable=false) Status status=Status.QUEUED;
    @Column(nullable=false,precision=10,scale=2) BigDecimal total;
    @Column(nullable=false) Instant createdAt=Instant.now();
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL,orphanRemoval=true) List<OrderItem> items=new ArrayList<>();
    @Version Long version;
    public enum Status { QUEUED, PREPARING, READY, COLLECTED, CANCELLED }
    protected FoodOrder() {}
}
