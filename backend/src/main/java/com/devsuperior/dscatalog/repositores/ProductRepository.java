package com.devsuperior.dscatalog.repositores;

import com.devsuperior.dscatalog.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
