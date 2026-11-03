package cl.gestortareas.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(PasswordResetMailService.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String from;

    public PasswordResetMailService(
        JavaMailSender mailSender,
        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
        @Value("${app.mail.from:no-reply@gestor-tareas.local}") String from
    ) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    public void sendPasswordReset(String recipient, String token) {
        String resetUrl = frontendUrl
            + "/restablecer-contrasena?token="
            + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Restablece tu contraseña de GestorTareas");
        message.setText("""
            Recibimos una solicitud para restablecer tu contraseña.

            Abre el siguiente enlace durante los próximos 15 minutos:
            %s

            Si no solicitaste este cambio, puedes ignorar este mensaje.
            """.formatted(resetUrl));

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            LOGGER.error(
                "No fue posible enviar el correo de recuperación a {}",
                recipient,
                exception
            );
        }
    }
}

