package br.com.techchallenge.functions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Azure Function: Relatório Semanal de Feedbacks
 *
 * Recebe dados completos dos últimos 7 dias do Quarkus (via scheduler toda
 * segunda-feira às 11h) e envia relatório por e-mail com:
 * - Lista individual de feedbacks (descrição, urgência, data de envio)
 * - Quantidade de avaliações por urgência
 * - Quantidade de avaliações por dia
 */
@SuppressWarnings("unused")
public class RelatorioSemanalFunction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class FeedbackItem {
        public String descricao;
        public String urgencia;
        public String dataEnvio;
    }

    public static class DiaResumo {
        public String data;
        public long critico;
        public long moderado;
        public long baixo;
        public long total;
    }

    public static class UrgenciaResumo {
        public long CRITICO;
        public long MODERADO;
        public long BAIXO;
    }

    public static class RelatorioRequest {
        public int totalFeedbacks;
        public double mediaAvaliacoes;
        public String periodoInicio;
        public String periodoFim;
        public FeedbackItem[] feedbacks;
        public UrgenciaResumo quantidadePorUrgencia;
        public DiaResumo[] quantidadePorDia;
    }

    @FunctionName("relatorio-semanal")
    public HttpResponseMessage executar(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "relatorio-semanal") HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("=== Azure Function: relatorio-semanal disparada ===");

        String body = request.getBody().orElse("");

        if (body.isBlank()) {
            log.warning("Body da requisição está vazio");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Body não pode ser vazio")
                    .build();
        }

        try {
            RelatorioRequest relatorio = MAPPER.readValue(body, RelatorioRequest.class);

            log.info(String.format(
                    "Relatório recebido | total=%d | período: %s até %s",
                    relatorio.totalFeedbacks, relatorio.periodoInicio, relatorio.periodoFim));

            enviarEmailComFallback(relatorio, context);

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Relatório semanal processado com sucesso")
                    .build();

        } catch (Exception e) {
            log.severe("Erro ao processar relatório: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage())
                    .build();
        }
    }

    private void enviarEmailComFallback(RelatorioRequest rel, ExecutionContext context) {

        Logger log = context.getLogger();

        String smtpHost = System.getenv("SMTP_HOST");
        String smtpPort = System.getenv("SMTP_PORT");
        String smtpUser = System.getenv("SMTP_USER");
        String smtpPassword = System.getenv("SMTP_PASSWORD");
        String destino = System.getenv("EMAIL_DESTINATARIO");
        String remetente = System.getenv("EMAIL_REMETENTE");

        String assunto = String.format(
                "📊 Relatório Semanal de Feedbacks | %s a %s",
                rel.periodoInicio, rel.periodoFim);

        String corpo = montarCorpoEmail(rel);

        try {
            if (smtpHost == null || smtpHost.isBlank() ||
                    smtpUser == null || smtpUser.isBlank() ||
                    smtpPassword == null || smtpPassword.isBlank()) {
                throw new IllegalStateException(
                        "Credenciais SMTP ausentes (SMTP_HOST/SMTP_USER/SMTP_PASSWORD)");
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", smtpPort != null ? smtpPort : "2525");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, smtpPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(
                    remetente != null ? remetente : "relatorios@feedback-api.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(destino != null ? destino : smtpUser));
            message.setSubject(assunto);
            message.setText(corpo, "UTF-8");

            Transport.send(message);
            log.info("✅ E-mail de relatório semanal enviado via SMTP");

        } catch (Exception e) {
            // FALLBACK: imprime e-mail completo no log — aplicação não quebra
            log.warning("⚠️ FALLBACK ATIVADO — SMTP indisponível: " + e.getMessage());
            log.info("============================================================");
            log.info("📧 E-MAIL QUE SERIA ENVIADO (FALLBACK DE LOG):");
            log.info("------------------------------------------------------------");
            log.info("DE:      " + (remetente != null ? remetente : "relatorios@feedback-api.com"));
            log.info("PARA:    " + (destino != null ? destino : "N/A"));
            log.info("ASSUNTO: " + assunto);
            log.info("CORPO:");
            log.info(corpo);
            log.info("============================================================");
        }
    }

    private String montarCorpoEmail(RelatorioRequest rel) {
        StringBuilder sb = new StringBuilder();
        String sep = "=".repeat(60);
        String div = "-".repeat(60);

        sb.append("📊 RELATÓRIO SEMANAL DE FEEDBACKS\n");
        sb.append(sep).append("\n\n");
        sb.append(String.format("Período:           %s a %s%n", rel.periodoInicio, rel.periodoFim));
        sb.append(String.format("Total de feedbacks: %d%n", rel.totalFeedbacks));
        sb.append(String.format("Média de avaliações: %.1f%n%n", rel.mediaAvaliacoes));

        // --- Quantidade por urgência ---
        sb.append("📌 QUANTIDADE POR URGÊNCIA:\n");
        sb.append(div).append("\n");
        if (rel.quantidadePorUrgencia != null) {
            sb.append(String.format("  🔴 CRITICO:  %d%n", rel.quantidadePorUrgencia.CRITICO));
            sb.append(String.format("  🟡 MODERADO: %d%n", rel.quantidadePorUrgencia.MODERADO));
            sb.append(String.format("  🟢 BAIXO:    %d%n", rel.quantidadePorUrgencia.BAIXO));
        }
        sb.append("\n");

        // --- Quantidade por dia ---
        sb.append("📅 QUANTIDADE POR DIA:\n");
        sb.append(div).append("\n");
        if (rel.quantidadePorDia != null) {
            for (DiaResumo dia : rel.quantidadePorDia) {
                sb.append(String.format(
                        "  %s → Total: %d  (🔴 %d | 🟡 %d | 🟢 %d)%n",
                        dia.data, dia.total, dia.critico, dia.moderado, dia.baixo));
            }
        }
        sb.append("\n");

        // --- Lista individual de feedbacks ---
        sb.append("📋 FEEDBACKS INDIVIDUAIS:\n");
        sb.append(div).append("\n");
        if (rel.feedbacks != null) {
            int i = 1;
            for (FeedbackItem fb : rel.feedbacks) {
                String emoji = "CRITICO".equals(fb.urgencia) ? "🔴"
                        : "MODERADO".equals(fb.urgencia) ? "🟡" : "🟢";
                sb.append(String.format("  %d. [%s %s] %s | %s%n",
                        i++, emoji, fb.urgencia, fb.descricao, fb.dataEnvio));
            }
        }

        sb.append("\n").append(sep).append("\n");
        sb.append("Relatório gerado automaticamente pelo sistema Feedback API.\n");

        return sb.toString();
    }
}
