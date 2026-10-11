package com.akshara.mart.model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    public Long id;
    public String name;
    public String category;
    @Column(length = 1000)
    public String description;
    public double price;
    public double mrp;
    public int stock;
    @Column(length = 600)
    public String imageUrl;
    public Long sellerId;
    public String sellerName;
}
