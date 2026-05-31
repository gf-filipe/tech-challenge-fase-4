package br.com.techchallenge.infra.web;

import br.com.techchallenge.application.usecase.GerarRelatorioSemanalUseCase;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/relatorio")
@Produces(MediaType.APPLICATION_JSON)
public class RelatorioResource {

    @Inject
    GerarRelatorioSemanalUseCase gerarRelatorioSemanalUseCase;

    @POST
    @Path("/disparar")
    public Response dispararManualmente() {
        // Dispara o mesmo caso de uso que o cron rodaria na segunda-feira
        gerarRelatorioSemanalUseCase.executar();

        return Response.ok("Relatório semanal disparado com sucesso! Verifique a Azure Function e o Mailtrap.").build();
    }
}
