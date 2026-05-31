package br.com.techchallenge.infra.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "fn-alerta-api")
public interface AlertaUrgenciaClient {

    @POST
    @Path("/api/alerta-urgencia")
    @Consumes(MediaType.APPLICATION_JSON)
    void enviarAlerta(String body);
}
