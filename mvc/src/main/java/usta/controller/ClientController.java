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
import usta.model.ProductRepository;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
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
        // Se sincroniza la colección: se limpian las asignaciones previas
        // y se agregan únicamente los productos marcados en el formulario.
        new HashSet<>(client.getPurchasedProducts()).forEach(client::removeProduct);
        if (productIds != null){
            for (Long productId : productIds){
                productRepository.findByIdOptional(productId).ifPresent(client::addProduct);
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
