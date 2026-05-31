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
class AlertaUrgenciaFunctionTest {

    @Mock
    private HttpRequestMessage<Optional<String>> request;

    @Mock
    private ExecutionContext context;

    @Mock
    private HttpResponseMessage.Builder responseBuilder;

    @Mock
    private HttpResponseMessage response;

    private AlertaUrgenciaFunction function;

    @BeforeEach
    void setUp() {
        function = new AlertaUrgenciaFunction();

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
    void deveProcessarComSucessoQuandoSMTPAusenteEAtivaFallback() {
        String payload = """
                {
                    "descricao": "Sistema completamente fora do ar",
                    "urgencia": "CRITICO",
                    "dataEnvio": "2026-05-31T10:00:00"
                }
                """;

        when(request.getBody()).thenReturn(Optional.of(payload));

        assertDoesNotThrow(() -> function.executar(request, context));

        verify(request).createResponseBuilder(HttpStatus.OK);
    }

    @Test
    void deveDesserializarJsonCorretamente() {
        String payload = """
                {
                    "descricao": "Aula impossível de acompanhar",
                    "urgencia": "CRITICO",
                    "dataEnvio": "2026-05-31T08:30:00"
                }
                """;

        when(request.getBody()).thenReturn(Optional.of(payload));

        assertDoesNotThrow(() -> function.executar(request, context));
        verify(request).createResponseBuilder(HttpStatus.OK);
    }

    @Test
    void deveRetornarErroInternoParaJsonInvalido() {
        when(request.getBody()).thenReturn(Optional.of("{ json inválido aqui }"));

        HttpResponseMessage resultado = function.executar(request, context);

        verify(request).createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR);
        assertNotNull(resultado);
    }
}
