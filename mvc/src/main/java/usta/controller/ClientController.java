package usta.controller;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import usta.model.Client;
import usta.model.ClientRepository;
import usta.model.Product;
import usta.model.ProductRepository;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Path("/clientes")
public class ClientController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    @Inject
    @Location("clientes/index.html")
    Template index;

    @Inject
    @Location("clientes/form.html")
    Template form;

    @Inject
    @Location("clientes/edit.html")
    Template edit;

    @Inject
    ClientRepository repository;

    @Inject
    ProductRepository productRepository;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return index.data("clients", repository.listAllClients());
    }

    @GET
    @Path("/nuevo")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance create() {
        return form.data("client", new Client());
    }

    @POST
    @Path("/guardar")
    @Transactional
    public Response save(@FormParam("nombre") String name,
                          @FormParam("email") String email){
        Client client = new Client(validateName(name), validateEmail(email));
        repository.save(client);
        return redirectToList();
    }

    @GET
    @Path("/editar/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@PathParam("id") Long id){
        Client client = repository.findByIdOptional(id).orElseThrow(()->new NotFoundException("Cliente no encontrado"));
        return edit.data("client", client)
                .data("products", productRepository.listAllProducts());
    }

    @POST
    @Path("/actualizar")
    @Transactional
    public Response update(@FormParam("id") Long id,
                            @FormParam("nombre") String name,
                            @FormParam("email") String email,
                            @FormParam("productos") List<Long> productIds){
        Client client = repository.findByIdOptional(id).orElseThrow(()->new NotFoundException("Cliente no encontrado"));
        client.setName(validateName(name));
        client.setEmail(validateEmail(email));
        syncPurchasedProducts(client, productIds);
        repository.save(client);
        return redirectToList();
    }

    @GET
    @Path("/eliminar/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id){
        repository.delete(id);
        return redirectToList();
    }

    private void syncPurchasedProducts(Client client, List<Long> productIds){
        // Se compara la selección anterior contra la nueva para mover el stock:
        // los productos que se agregan descuentan 1 unidad (validando que haya stock);
        // los que se quitan devuelven 1 unidad. Los que no cambian, se dejan igual.
        Set<Long> previousIds = new HashSet<>();
        for (Product product : client.getPurchasedProducts()){
            previousIds.add(product.getId());
        }
        Set<Long> selectedIds = new HashSet<>();
        if (productIds != null){
            selectedIds.addAll(productIds);
        }

        // Productos que se quitan: se devuelve el stock.
        for (Product product : new HashSet<>(client.getPurchasedProducts())){
            if (!selectedIds.contains(product.getId())){
                product.setStock(product.getStock() + 1);
                client.removeProduct(product);
            }
        }

        // Productos que se agregan: se valida y se descuenta el stock.
        for (Long productId : selectedIds){
            if (!previousIds.contains(productId)){
                Product product = productRepository.findByIdOptional(productId)
                        .orElseThrow(() -> new jakarta.ws.rs.BadRequestException("Producto no encontrado"));
                if (product.getStock() == null || product.getStock() <= 0){
                    throw new jakarta.ws.rs.BadRequestException(
                            "No hay stock disponible del producto \"" + product.getName() + "\"");
                }
                product.setStock(product.getStock() - 1);
                client.addProduct(product);
            }
        }
    }

    private String validateName(String name){
        if (name == null || name.isBlank()){
            throw new jakarta.ws.rs.BadRequestException("El nombre es obligatorio");
        }
        return name.trim();
    }

    private String validateEmail(String email){
        if (email == null || email.isBlank()){
            throw new jakarta.ws.rs.BadRequestException("El correo es obligatorio");
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()){
            throw new jakarta.ws.rs.BadRequestException("El correo no tiene un formato válido");
        }
        return trimmed;
    }

    private Response redirectToList(){
        return Response.seeOther(URI.create("/clientes")).build();
    }
}
