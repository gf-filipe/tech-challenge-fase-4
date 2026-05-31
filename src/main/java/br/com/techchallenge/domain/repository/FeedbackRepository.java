package br.com.techchallenge.domain.repository;

import br.com.techchallenge.domain.entity.Feedback;

import java.util.List;

public interface FeedbackRepository {

    void salvar(Feedback feedback);

    List<Feedback> buscarUltimosDias(int dias);
}