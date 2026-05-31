package br.com.techchallenge.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Feedback {

    @Id
    @GeneratedValue
    public Long id;

    public String descricao;
    public int nota;

    @Enumerated(EnumType.STRING)
    public Urgencia urgencia;

    public LocalDateTime dataEnvio;

    public enum Urgencia {
        CRITICO, MODERADO, BAIXO
    }
}