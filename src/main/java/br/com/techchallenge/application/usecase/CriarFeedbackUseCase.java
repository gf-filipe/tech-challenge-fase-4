package br.com.techchallenge.application.usecase;

import br.com.techchallenge.domain.entity.Feedback;
import br.com.techchallenge.domain.entity.Feedback.Urgencia;
import br.com.techchallenge.domain.repository.FeedbackRepository;
import br.com.techchallenge.infra.client.AlertaUrgenciaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@ApplicationScoped
public class CriarFeedbackUseCase {

    private static final Logger LOG = Logger.getLogger(CriarFeedbackUseCase.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    FeedbackRepository repository;

    @Inject
    @RestClient
    AlertaUrgenciaClient alertaClient;

    public Feedback executar(String descricao, int nota) {

        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição não pode ser vazia");
        }

        if (nota < 0 || nota > 10) {
            throw new IllegalArgumentException("Nota deve estar entre 0 e 10");
        }

        Urgencia urgencia = calcularUrgencia(nota);

        Feedback feedback = salvarFeedback(descricao, nota, urgencia);

        if (urgencia == Urgencia.CRITICO) {
            enviarAlerta(feedback);
        }

        return feedback;
    }

    @Transactional
    Feedback salvarFeedback(String descricao, int nota, Urgencia urgencia) {

        Feedback feedback = new Feedback();
        feedback.descricao = descricao;
        feedback.nota = nota;
        feedback.urgencia = urgencia;
        feedback.dataEnvio = LocalDateTime.now();

        repository.salvar(feedback);

        return feedback;
    }

    private Urgencia calcularUrgencia(int nota) {
        if (nota <= 3)
            return Urgencia.CRITICO;
        if (nota <= 6)
            return Urgencia.MODERADO;
        return Urgencia.BAIXO;
    }

    private void enviarAlerta(Feedback feedback) {

        try {
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("descricao", feedback.descricao);
            payload.put("urgencia", feedback.urgencia.name());
            payload.put("dataEnvio",
                    feedback.dataEnvio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            String json = MAPPER.writeValueAsString(payload);

            LOG.infof("Disparando alerta Azure Function | urgencia=%s | descricao=%s",
                    feedback.urgencia, feedback.descricao);

            alertaClient.enviarAlerta(json);

            LOG.info("Alerta enviado com sucesso para Azure Function");

        } catch (Exception e) {
            LOG.errorf("Falha ao enviar alerta para Azure Function: %s", e.getMessage());
        }
    }
}