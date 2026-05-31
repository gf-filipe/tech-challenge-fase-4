package br.com.techchallenge.infra.scheduler;

import br.com.techchallenge.application.usecase.GerarRelatorioSemanalUseCase;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Scheduler responsável por disparar o relatório semanal de feedbacks.
 * Cron configurado em application.properties: relatorio.cron
 * Padrão: toda segunda-feira às 11h (0 0 11 ? * MON *)
 */
@ApplicationScoped
public class RelatorioScheduler {

    private static final Logger LOG = Logger.getLogger(RelatorioScheduler.class);

    @Inject
    GerarRelatorioSemanalUseCase gerarRelatorioSemanalUseCase;

    @Scheduled(cron = "{relatorio.cron}")
    public void dispararRelatorioSemanal() {
        LOG.info("⏰ Scheduler ativado — iniciando geração do relatório semanal");
        gerarRelatorioSemanalUseCase.executar();
    }
}
