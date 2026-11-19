package cl.gestortareas.api.service;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.repository.AppUserRepository;

@SpringBootTest
class RefreshTokenServiceIntegrationTests {
    @Autowired RefreshTokenService tokens;
    @Autowired AppUserRepository users;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void serializesConcurrentRotationAndRejectsTheConsumedToken() throws Exception {
        var user = users.saveAndFlush(new AppUser("Rotación concurrente", "concurrent-refresh@test.cl", "hash"));
        String raw = tokens.issue(user, "test", "127.0.0.1");
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var attempting = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                var rotated = tokens.consumeAndRotate(raw);
                locked.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for release");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                return rotated;
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> {
                attempting.countDown();
                try { tokens.consumeAndRotate(raw); return false; }
                catch (ResponseStatusException error) {
                    assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
                    return true;
                }
            });
            assertTrue(attempting.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            var renewed = first.get(5, TimeUnit.SECONDS);
            assertNotNull(renewed);
            assertTrue(second.get(5, TimeUnit.SECONDS));
            assertNotNull(tokens.consumeAndRotate(renewed.rawToken()));
        } finally {
            release.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
            tokens.revokeAll(user);
            users.deleteById(user.getId());
        }
    }
}
