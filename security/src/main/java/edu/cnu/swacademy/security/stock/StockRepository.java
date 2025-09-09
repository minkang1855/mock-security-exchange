package edu.cnu.swacademy.security.stock;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

  Optional<Stock> findByCode(String code);

  Optional<Stock> findByName(String name);

  boolean existsByCode(String code);

  boolean existsByName(String name);
}
