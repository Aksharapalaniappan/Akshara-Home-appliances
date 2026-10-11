package com.akshara.mart.repo;

import com.akshara.mart.model.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepo extends JpaRepository<Orders, Long> {
    List<Orders> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Orders> findAllByOrderByCreatedAtDesc();
}
