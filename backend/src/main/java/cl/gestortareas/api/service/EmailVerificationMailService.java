package cl.gestortareas.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailService {

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String from;

    public EmailVerificationMailService(
        JavaMailSender mailSender,
        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
        @Value("${app.mail.from:no-reply@gestor-tareas.local}") String from
    ) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.from = from;
    }

    public void sendVerification(String recipient, String token) {
        String verificationUrl = frontendUrl + "/verificar-correo?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject("Confirma tu correo de GestorTareas");
        message.setText("""
            Gracias por crear tu cuenta.

            Confirma tu correo durante las próximas 24 horas:
            %s

            Si no creaste esta cuenta, puedes ignorar este mensaje.
            """.formatted(verificationUrl));
        mailSender.send(message);
    }
}
