package usta.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/")
public class HomeController {

    @GET
    public Response home() {
        return Response.seeOther(URI.create("/clientes")).build();
    }
}
