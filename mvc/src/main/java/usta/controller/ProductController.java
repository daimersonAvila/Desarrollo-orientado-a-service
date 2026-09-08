package usta.controller;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import usta.model.Product;
import usta.model.ProductRepository;

import java.math.BigDecimal;
import java.net.URI;

@Path("/productos")
public class ProductController {

    @Inject
    @Location("index.html")
    Template index;

    @Inject
    @Location("create.html")
    Template create;

    @Inject
    @Location("edit.html")
    Template edit;

    @Inject
    ProductRepository repository;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return index.data("products", repository.listAllProducts());
    }

    @GET
    @Path("/nuevo")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance create() {
        return create.data("product", new Product());
    }

    @POST
    @Path("/guardar")
    @Transactional
    public Response save(@FormParam("nombre") String name,
                         @FormParam("precio") String price,
                         @FormParam("stock") String stock){
        Product product = new Product(validateName(name), validatePrice(price), validateStock(stock));
        repository.save(product);
        return redirectToList();
    }

    @GET
    @Path("/editar/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance edit(@PathParam("id") Long id){
        Product product = repository.findByIdOptional(id).orElseThrow(()->new NotFoundException("Producto no encontrado"));
        return edit.data("product", product);
    }

    @POST
    @Path("/actualizar")
    @Transactional
    public Response update(@FormParam("id") Long id,
                           @FormParam("nombre") String name,
                           @FormParam("precio") String price,
                           @FormParam("stock") String stock){
        Product product = repository.findByIdOptional(id).orElseThrow(()->new NotFoundException("Producto no encontrado"));
        product.setName(validateName(name));
        product.setPrice(validatePrice(price));
        product.setStock(validateStock(stock));
        repository.save(product);
        return redirectToList();
    }

    @GET
    @Path("/eliminar/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id){
        repository.delete(id);
        return redirectToList();
    }

    private String validateName(String name){
        if (name == null || name.isBlank()){
            throw new jakarta.ws.rs.BadRequestException("El nombre es obligatorio");
        }
        return name.trim();
    }

    private BigDecimal validatePrice(String price){
        if (price == null || price.isBlank()){
            throw new jakarta.ws.rs.BadRequestException("El precio debe ser obligatorio");
        }
        try{
            return new BigDecimal(price);
        } catch (NumberFormatException e) {
            throw new jakarta.ws.rs.BadRequestException("El precio debe ser un número válido");
        }
    }

    private Integer validateStock(String stock){
        if (stock == null || stock.isBlank()){
            throw new jakarta.ws.rs.BadRequestException("El stock es obligatorio");
        }
        try{
            int value = Integer.parseInt(stock.trim());
            if (value < 0){
                throw new jakarta.ws.rs.BadRequestException("El stock debe ser mayor o igual a 0");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new jakarta.ws.rs.BadRequestException("El stock debe ser un número entero válido");
        }
    }

    private Response redirectToList(){
        return Response.seeOther(URI.create("/productos")).build();
    }
}
