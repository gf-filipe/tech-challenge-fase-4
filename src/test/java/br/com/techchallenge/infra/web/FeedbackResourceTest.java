package br.com.techchallenge.infra.web;

import br.com.techchallenge.application.usecase.CriarFeedbackUseCase;
import br.com.techchallenge.domain.entity.Feedback;
import br.com.techchallenge.domain.entity.Feedback.Urgencia;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
class FeedbackResourceTest {

    @InjectMock
    CriarFeedbackUseCase useCase;

    @Test
    void deveCriarFeedbackComSucesso() {
        Feedback feedbackMock = new Feedback();
        feedbackMock.id = 1L;
        feedbackMock.descricao = "Ótimo produto";
        feedbackMock.nota = 9;
        feedbackMock.urgencia = Urgencia.BAIXO;
        feedbackMock.dataEnvio = LocalDateTime.now();

        when(useCase.executar(anyString(), anyInt())).thenReturn(feedbackMock);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "descricao": "Ótimo produto",
                          "nota": 9
                        }
                        """)
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("descricao", equalTo("Ótimo produto"))
                .body("nota", equalTo(9));
    }

    @Test
    void deveRetornarBadRequestParaBodyVazio() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(400);
    }

    @Test
    void deveRetornarBadRequestParaDescricaoVazia() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "descricao": "",
                          "nota": 5
                        }
                        """)
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(400);
    }

    @Test
    void deveRetornarBadRequestParaDescricaoNula() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nota": 5
                        }
                        """)
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(400);
    }

    @Test
    void deveChamarUseCaseComParametrosCorretos() {
        Feedback feedbackMock = new Feedback();
        feedbackMock.id = 2L;
        feedbackMock.descricao = "Ruim demais";
        feedbackMock.nota = 1;
        feedbackMock.urgencia = Urgencia.CRITICO;
        feedbackMock.dataEnvio = LocalDateTime.now();

        when(useCase.executar("Ruim demais", 1)).thenReturn(feedbackMock);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "descricao": "Ruim demais",
                          "nota": 1
                        }
                        """)
                .when()
                .post("/avaliacao")
                .then()
                .statusCode(200);

        verify(useCase, times(1)).executar("Ruim demais", 1);
    }
}
