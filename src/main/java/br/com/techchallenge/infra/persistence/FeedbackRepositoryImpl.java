package br.com.techchallenge.infra.persistence;

import br.com.techchallenge.domain.entity.Feedback;
import br.com.techchallenge.domain.repository.FeedbackRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class FeedbackRepositoryImpl implements FeedbackRepository, PanacheRepository<Feedback> {

    @Override
    public void salvar(Feedback feedback) {
        persist(feedback);
    }

    @Override
    public List<Feedback> buscarUltimosDias(int dias) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        return list("dataEnvio >= ?1", desde);
    }
}