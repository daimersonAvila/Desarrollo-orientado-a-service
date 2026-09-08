package usta.model;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClientRepository implements PanacheRepository<Client> {

    public List<Client> listAllClients() {
        return listAll();
    }

    public Optional<Client> findByIdOptional(Long id) {
        return find("id", id).firstResultOptional();
    }

    public Client save(Client client) {
        persist(client);
        return client;
    }

    public boolean delete(Long id){
        return deleteById(id);
    }
}
