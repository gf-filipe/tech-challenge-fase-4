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
 * Azure Function: Alerta de Urgência
 *
 * Recebe um webhook HTTP POST do Quarkus após a criação de um Feedback
 * com urgência CRITICO ou MODERADO. Envia e-mail via SMTP (Mailtrap).
 */
@SuppressWarnings("unused")
public class AlertaUrgenciaFunction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class AlertaRequest {
        public String descricao;
        public String urgencia;
        public String dataEnvio;
    }

    @FunctionName("alerta-urgencia")
    public HttpResponseMessage executar(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS, route = "alerta-urgencia") HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {

        Logger log = context.getLogger();
        log.info("=== Azure Function: alerta-urgencia disparada ===");

        String body = request.getBody().orElse("");

        if (body.isBlank()) {
            log.warning("Body da requisição está vazio");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Body não pode ser vazio")
                    .build();
        }

        try {
            AlertaRequest alerta = MAPPER.readValue(body, AlertaRequest.class);

            log.info(String.format(
                    "Alerta recebido | urgencia=%s | descricao=%s | dataEnvio=%s",
                    alerta.urgencia, alerta.descricao, alerta.dataEnvio));

            enviarEmailComFallback(alerta, context);

            return request.createResponseBuilder(HttpStatus.OK)
                    .body("Alerta processado com sucesso")
                    .build();

        } catch (Exception e) {
            log.severe("Erro ao processar alerta: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro interno: " + e.getMessage())
                    .build();
        }
    }

    private void enviarEmailComFallback(AlertaRequest alerta, ExecutionContext context) {

        Logger log = context.getLogger();

        String smtpHost = System.getenv("SMTP_HOST");
        String smtpPort = System.getenv("SMTP_PORT");
        String smtpUser = System.getenv("SMTP_USER");
        String smtpPassword = System.getenv("SMTP_PASSWORD");
        String destino = System.getenv("EMAIL_DESTINATARIO");
        String remetente = System.getenv("EMAIL_REMETENTE");

        String assunto = String.format("[ALERTA %s] Novo Feedback Crítico", alerta.urgencia);
        String corpo = String.format(
                "⚠️ ALERTA DE FEEDBACK - URGÊNCIA: %s\n\n" +
                        "Descrição: %s\n" +
                        "Urgência:  %s\n" +
                        "Data:      %s\n\n" +
                        "Acesse o painel para tomar providências.",
                alerta.urgencia,
                alerta.descricao,
                alerta.urgencia,
                alerta.dataEnvio);

        try {
            if (smtpHost == null || smtpHost.isBlank() ||
                    smtpUser == null || smtpUser.isBlank() ||
                    smtpPassword == null || smtpPassword.isBlank()) {
                throw new IllegalStateException(
                        "Credenciais SMTP ausentes ou nulas (SMTP_HOST/SMTP_USER/SMTP_PASSWORD)");
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
                    remetente != null ? remetente : "alertas@feedback-api.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(destino != null ? destino : smtpUser));
            message.setSubject(assunto);
            message.setText(corpo, "UTF-8");

            Transport.send(message);
            log.info("✅ E-mail de alerta enviado via SMTP com sucesso");

        } catch (Exception e) {
            // FALLBACK OBRIGATÓRIO: imprime o e-mail completo no log
            log.warning("⚠️ FALLBACK ATIVADO — Não foi possível enviar e-mail via SMTP: " + e.getMessage());
            log.info("============================================================");
            log.info("📧 E-MAIL QUE SERIA ENVIADO (FALLBACK DE LOG):");
            log.info("------------------------------------------------------------");
            log.info("DE:      " + (remetente != null ? remetente : "alertas@feedback-api.com"));
            log.info("PARA:    " + (destino != null ? destino : "N/A"));
            log.info("ASSUNTO: " + assunto);
            log.info("CORPO:");
            log.info(corpo);
            log.info("============================================================");
        }
    }
}
