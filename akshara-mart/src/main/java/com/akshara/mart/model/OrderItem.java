package com.akshara.mart.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long productId;
    public String productName;
    @Column(length = 600)
    public String imageUrl;
    public double price;
    public int quantity;
    public Long sellerId;
}
