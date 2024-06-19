package project.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.domain.Product;

@Repository
public interface ProductDao extends JpaRepository<Product, Long> {

}