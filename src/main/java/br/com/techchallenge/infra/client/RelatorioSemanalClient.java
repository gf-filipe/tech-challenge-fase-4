package br.com.techchallenge.infra.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "fn-relatorio-api")
public interface RelatorioSemanalClient {

    @POST
    @Path("/api/relatorio-semanal")
    @Consumes(MediaType.APPLICATION_JSON)
    void enviarRelatorio(String body);
}
