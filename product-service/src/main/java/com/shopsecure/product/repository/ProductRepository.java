package com.shopsecure.product.repository;

import com.shopsecure.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data uses parameterised queries, so user input never gets concatenated into SQL
 * (SQL-injection safe). Never build JPQL/SQL strings with "+" and request values.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("select distinct p.category from Product p where p.category is not null order by p.category")
    List<String> findCategories();
}
