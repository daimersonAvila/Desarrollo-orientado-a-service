package usta.model;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public List<Product> listAllProducts() {
        return listAll();
    }

    public Optional<Product> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public Product save(Product product) {
        persist(product);
        return product;
    }

    public boolean delete(Long id){
        return deleteById(id);
    }
}
