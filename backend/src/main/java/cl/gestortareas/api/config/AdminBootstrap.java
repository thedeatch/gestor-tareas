package cl.gestortareas.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cl.gestortareas.api.model.UserRole;
import cl.gestortareas.api.repository.AppUserRepository;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        AdminBootstrap.class
    );

    private final AppUserRepository userRepository;
    private final String adminEmail;

    public AdminBootstrap(
        AppUserRepository userRepository,
        @Value("${app.bootstrap-admin-email:}") String adminEmail
    ) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank()
            || userRepository.countByRole(UserRole.ADMIN) > 0) {
            return;
        }

        userRepository.findByEmailIgnoreCase(adminEmail).ifPresentOrElse(
            user -> {
                user.changeRole(UserRole.ADMIN);
                LOGGER.info("Administrador inicial configurado correctamente");
            },
            () -> LOGGER.warn(
                "BOOTSTRAP_ADMIN_EMAIL no coincide con una cuenta existente"
            )
        );
    }
}
