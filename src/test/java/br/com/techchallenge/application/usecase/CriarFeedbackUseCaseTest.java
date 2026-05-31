package br.com.techchallenge.application.usecase;

import br.com.techchallenge.domain.entity.Feedback;
import br.com.techchallenge.domain.entity.Feedback.Urgencia;
import br.com.techchallenge.domain.repository.FeedbackRepository;
import br.com.techchallenge.infra.client.AlertaUrgenciaClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CriarFeedbackUseCaseTest {

    @Mock
    FeedbackRepository repository;

    @Mock
    AlertaUrgenciaClient alertaClient;

    @InjectMocks
    CriarFeedbackUseCase useCase;

    @BeforeEach
    void setUp() {
    }

    @Test
    void deveCriarFeedbackComUrgenciaCriticoParaNotaBaixa() {
        doNothing().when(repository).salvar(any(Feedback.class));
        Feedback resultado = useCase.executar("Aula péssima, conteúdo confuso", 2);

        assertNotNull(resultado);
        assertEquals("Aula péssima, conteúdo confuso", resultado.descricao);
        assertEquals(2, resultado.nota);
        assertEquals(Urgencia.CRITICO, resultado.urgencia);
        assertNotNull(resultado.dataEnvio);
    }

    @Test
    void deveCriarFeedbackComUrgenciaModeraoParaNotaMedia() {
        doNothing().when(repository).salvar(any(Feedback.class));
        Feedback resultado = useCase.executar("Aula razoável", 5);

        assertEquals(Urgencia.MODERADO, resultado.urgencia);
    }

    @Test
    void deveCriarFeedbackComUrgenciaBaixoParaNotaAlta() {
        doNothing().when(repository).salvar(any(Feedback.class));
        Feedback resultado = useCase.executar("Excelente aula!", 9);

        assertEquals(Urgencia.BAIXO, resultado.urgencia);
    }

    @Test
    void deveDispararAlertaApenaParaUrgenciaCritica() {
        doNothing().when(repository).salvar(any(Feedback.class));
        useCase.executar("Aula impossível de acompanhar", 1);

        verify(alertaClient, times(1)).enviarAlerta(anyString());
    }

    @Test
    void naoDeveDispararAlertaParaUrgenciaModeraoOuBaixo() {
        doNothing().when(repository).salvar(any(Feedback.class));
        useCase.executar("Aula ok", 5); // MODERADO
        useCase.executar("Excelente!", 10); // BAIXO

        verify(alertaClient, never()).enviarAlerta(anyString());
    }

    @Test
    void deveEnviarJsonCorretoParaAlertaFunction() {
        doNothing().when(repository).salvar(any(Feedback.class));
        useCase.executar("Sistema fora do ar", 1);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(alertaClient).enviarAlerta(captor.capture());

        String json = captor.getValue();
        assertTrue(json.contains("\"descricao\""), "JSON deve conter campo 'descricao'");
        assertTrue(json.contains("\"urgencia\""), "JSON deve conter campo 'urgencia'");
        assertTrue(json.contains("\"dataEnvio\""), "JSON deve conter campo 'dataEnvio'");
        assertTrue(json.contains("CRITICO"), "JSON deve indicar urgência CRITICO");
        assertFalse(json.contains("\"nota\""), "JSON não deve expor o campo 'nota'");
    }

    @Test
    void deveLancarExcecaoParaDescricaoVazia() {
        assertThrows(IllegalArgumentException.class, () -> useCase.executar("", 5));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar(null, 5));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar("   ", 5));
    }

    @Test
    void deveLancarExcecaoParaNotaForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.executar("Desc", -1));
        assertThrows(IllegalArgumentException.class, () -> useCase.executar("Desc", 11));
    }

    @Test
    void deveNaoQuebrarSeAzureFunctionFalhar() {
        doNothing().when(repository).salvar(any(Feedback.class));
        doThrow(new RuntimeException("Azure Down"))
                .when(alertaClient).enviarAlerta(anyString());

        assertDoesNotThrow(() -> useCase.executar("Feedback crítico", 1));
    }

    @Test
    void deveSempePersistirNoRepositorioIndependenteDaUrgencia() {
        doNothing().when(repository).salvar(any(Feedback.class));
        useCase.executar("Aula crítica", 1); // CRITICO
        useCase.executar("Aula moderada", 5); // MODERADO
        useCase.executar("Aula ótima", 9); // BAIXO

        verify(repository, times(3)).salvar(any(Feedback.class));
    }
}
