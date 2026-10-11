
package com.akshara.mart.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    public Long id;

    @Column(name = "full_name")
    public String name;

    @Column(name = "email", unique = true)
    public String email;

    @JsonIgnore
    @Column(name = "password_hash")
    public String passwordHash;

    @Column(name = "role")
    public String role;

}