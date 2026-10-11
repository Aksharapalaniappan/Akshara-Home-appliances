package com.akshara.mart.repo;

import com.akshara.mart.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findBySellerId(Long sellerId);

    @Query("select p from Product p where lower(p.name) like lower(concat('%', :q, '%')) " +
           "or lower(p.category) like lower(concat('%', :q, '%')) " +
           "or lower(p.description) like lower(concat('%', :q, '%'))")
    List<Product> search(@Param("q") String q);
}
