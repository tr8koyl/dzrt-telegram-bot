package project.dao;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import project.domain.Product;

@Repository
public interface ProductDao extends JpaRepository<Product, Long> {

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE product_tb SET
    availability = :#{#product.availability}
    WHERE name = :#{#product.name}
    """, nativeQuery = true)
    void updateProductStatus(Product product);
}
