package com.ecommerces.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "items") // avoid loading lazy collection in toString
public class Order {

    /**
     * Convenience constructor used by CreateOrderUseCase to build a new order.
     * Items are added separately via {@link #addItem}.
     */
    public Order(String userId, UUID shopId, BigDecimal totalAmount) {
        this.userId = userId;
        this.shopId = shopId;
        this.totalAmount = totalAmount;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "shop_id", nullable = false)
    private UUID shopId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory helper that creates a new {@link OrderItem}, links it to this order,
     * and appends it to the items collection.
     * Relies on {@code CascadeType.ALL} — no separate repository call needed.
     */
    public void addItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrder(this);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        this.items.add(item);
    }
}
