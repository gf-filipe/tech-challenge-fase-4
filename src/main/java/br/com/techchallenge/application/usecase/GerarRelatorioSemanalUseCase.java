package br.com.techchallenge.application.usecase;

import br.com.techchallenge.domain.entity.Feedback;
import br.com.techchallenge.domain.entity.Feedback.Urgencia;
import br.com.techchallenge.domain.repository.FeedbackRepository;
import br.com.techchallenge.infra.client.RelatorioSemanalClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Usecase para gerar o relatório semanal de feedbacks.
 *
 * Inclui, conforme especificação do Tech Challenge:
 * - Lista individual de feedbacks (descrição, urgência, dataEnvio)
 * - Quantidade de avaliações por dia
 * - Quantidade de avaliações por urgência (CRITICO, MODERADO, BAIXO)
 */
@ApplicationScoped
public class GerarRelatorioSemanalUseCase {

    private static final Logger LOG = Logger.getLogger(GerarRelatorioSemanalUseCase.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Inject
    FeedbackRepository repository;

    @Inject
    @RestClient
    RelatorioSemanalClient relatorioClient;

    public void executar() {

        LOG.info("Gerando relatório semanal dos últimos 7 dias...");

        try {
            List<Feedback> feedbacks = repository.buscarUltimosDias(7);

            Map<LocalDate, Map<Urgencia, Long>> porDia = new LinkedHashMap<>();

            Map<Urgencia, Long> porUrgencia = new LinkedHashMap<>();
            porUrgencia.put(Urgencia.CRITICO, 0L);
            porUrgencia.put(Urgencia.MODERADO, 0L);
            porUrgencia.put(Urgencia.BAIXO, 0L);

            double somaNotas = 0.0;

            for (Feedback f : feedbacks) {
                if (f.dataEnvio == null)
                    continue;

                somaNotas += f.nota;

                Urgencia urg = f.urgencia != null ? f.urgencia : Urgencia.BAIXO;
                LocalDate dia = f.dataEnvio.toLocalDate();

                porDia.computeIfAbsent(dia, k -> new LinkedHashMap<>())
                        .merge(urg, 1L, Long::sum);

                porUrgencia.merge(urg, 1L, Long::sum);
            }

            ObjectNode root = MAPPER.createObjectNode();
            root.put("totalFeedbacks", feedbacks.size());
            root.put("mediaAvaliacoes", feedbacks.isEmpty() ? 0.0 : somaNotas / feedbacks.size());
            root.put("periodoInicio", LocalDate.now().minusDays(7).format(DATE_FMT));
            root.put("periodoFim", LocalDate.now().format(DATE_FMT));

            ArrayNode feedbacksNode = root.putArray("feedbacks");
            for (Feedback f : feedbacks) {
                ObjectNode fb = feedbacksNode.addObject();
                fb.put("descricao", f.descricao);
                fb.put("urgencia", f.urgencia != null ? f.urgencia.name() : "BAIXO");
                fb.put("dataEnvio", f.dataEnvio != null
                        ? f.dataEnvio.format(DATETIME_FMT)
                        : "");
            }

            ObjectNode urgenciaNode = root.putObject("quantidadePorUrgencia");
            urgenciaNode.put("CRITICO", porUrgencia.get(Urgencia.CRITICO));
            urgenciaNode.put("MODERADO", porUrgencia.get(Urgencia.MODERADO));
            urgenciaNode.put("BAIXO", porUrgencia.get(Urgencia.BAIXO));

            ArrayNode porDiaNode = root.putArray("quantidadePorDia");
            porDia.forEach((data, urgencias) -> {
                ObjectNode diaNode = porDiaNode.addObject();
                diaNode.put("data", data.format(DATE_FMT));
                diaNode.put("critico", urgencias.getOrDefault(Urgencia.CRITICO, 0L));
                diaNode.put("moderado", urgencias.getOrDefault(Urgencia.MODERADO, 0L));
                diaNode.put("baixo", urgencias.getOrDefault(Urgencia.BAIXO, 0L));
                diaNode.put("total",
                        urgencias.values().stream().mapToLong(Long::longValue).sum());
            });

            String json = MAPPER.writeValueAsString(root);

            LOG.infof("Enviando relatório semanal: %d feedbacks", feedbacks.size());
            relatorioClient.enviarRelatorio(json);
            LOG.info("Relatório semanal enviado com sucesso para Azure Function");

        } catch (Exception e) {
            LOG.errorf("Falha ao gerar/enviar relatório semanal: %s", e.getMessage());
        }
    }
}
