package com.akshara.mart.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Orders {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    public String userName;
    public double total;
    public String status; // PLACED, SHIPPED, DELIVERED
    @Column(length = 500)
    public String address;
    public String paymentMethod;
    public LocalDateTime createdAt;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    public List<OrderItem> items = new ArrayList<>();
}
