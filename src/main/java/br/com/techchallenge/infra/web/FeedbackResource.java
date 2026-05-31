package br.com.techchallenge.infra.web;

import br.com.techchallenge.application.usecase.CriarFeedbackUseCase;
import br.com.techchallenge.domain.entity.Feedback;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/avaliacao")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FeedbackResource {

    @Inject
    CriarFeedbackUseCase useCase;

    public static class Request {
        public String descricao;
        public int nota;
    }

    @POST
    public Response criar(Request request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Body da requisição está vazio ou inválido")
                    .build();
        }

        if (request.descricao == null || request.descricao.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Descrição não pode ser vazia")
                    .build();
        }

        Feedback feedback = useCase.executar(request.descricao, request.nota);

        return Response.ok(feedback).build();
    }
}