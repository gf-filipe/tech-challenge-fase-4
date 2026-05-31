package br.com.techchallenge.functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioSemanalFunctionTest {

    @Mock
    private HttpRequestMessage<Optional<String>> request;

    @Mock
    private ExecutionContext context;

    @Mock
    private HttpResponseMessage.Builder responseBuilder;

    @Mock
    private HttpResponseMessage response;

    private RelatorioSemanalFunction function;

    private static final String PAYLOAD_COMPLETO = """
            {
                "totalFeedbacks": 3,
                "mediaAvaliacoes": 5.3,
                "periodoInicio": "2026-05-24",
                "periodoFim": "2026-05-31",
                "feedbacks": [
                    {"descricao": "Aula ótima", "urgencia": "BAIXO", "dataEnvio": "2026-05-28T10:00:00"},
                    {"descricao": "Aula razoável", "urgencia": "MODERADO", "dataEnvio": "2026-05-29T14:00:00"},
                    {"descricao": "Péssimo conteúdo", "urgencia": "CRITICO", "dataEnvio": "2026-05-30T09:00:00"}
                ],
                "quantidadePorUrgencia": {"CRITICO": 1, "MODERADO": 1, "BAIXO": 1},
                "quantidadePorDia": [
                    {"data": "2026-05-28", "critico": 0, "moderado": 0, "baixo": 1, "total": 1},
                    {"data": "2026-05-29", "critico": 0, "moderado": 1, "baixo": 0, "total": 1},
                    {"data": "2026-05-30", "critico": 1, "moderado": 0, "baixo": 0, "total": 1}
                ]
            }
            """;

    @BeforeEach
    void setUp() {
        function = new RelatorioSemanalFunction();
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
        when(request.createResponseBuilder(any(HttpStatus.class))).thenReturn(responseBuilder);
        when(responseBuilder.body(any())).thenReturn(responseBuilder);
        when(responseBuilder.build()).thenReturn(response);
    }

    @Test
    void deveRetornarBadRequestParaBodyVazio() {
        when(request.getBody()).thenReturn(Optional.empty());

        HttpResponseMessage resultado = function.executar(request, context);

        verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
        assertNotNull(resultado);
    }

    @Test
    void deveRetornarBadRequestParaBodyEmBranco() {
        when(request.getBody()).thenReturn(Optional.of("   "));

        HttpResponseMessage resultado = function.executar(request, context);

        verify(request).createResponseBuilder(HttpStatus.BAD_REQUEST);
        assertNotNull(resultado);
    }

    @Test
    void deveProcessarPayloadCompletoComFallbackQuandoSMTPAusente() {
        when(request.getBody()).thenReturn(Optional.of(PAYLOAD_COMPLETO));
        assertDoesNotThrow(() -> function.executar(request, context));

        verify(request).createResponseBuilder(HttpStatus.OK);
    }

    @Test
    void deveDesserializarJsonComTodosOsCampos() {
        when(request.getBody()).thenReturn(Optional.of(PAYLOAD_COMPLETO));

        assertDoesNotThrow(() -> function.executar(request, context));
        verify(request).createResponseBuilder(HttpStatus.OK);
    }

    @Test
    void deveRetornarErroInternoParaJsonInvalido() {
        when(request.getBody()).thenReturn(Optional.of("{ json inválido }"));

        HttpResponseMessage resultado = function.executar(request, context);

        verify(request).createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR);
        assertNotNull(resultado);
    }

    @Test
    void deveProcessarPayloadMinimalSemFeedbacksNemDias() {
        String payloadMinimal = """
                {
                    "totalFeedbacks": 0,
                    "mediaAvaliacoes": 0.0,
                    "periodoInicio": "2026-05-24",
                    "periodoFim": "2026-05-31"
                }
                """;

        when(request.getBody()).thenReturn(Optional.of(payloadMinimal));

        assertDoesNotThrow(() -> function.executar(request, context));
        verify(request).createResponseBuilder(HttpStatus.OK);
    }
}
