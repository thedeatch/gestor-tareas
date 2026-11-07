package cl.gestortareas.api.controller;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import jakarta.annotation.PreDestroy;

@Component
class ChatLongPoll {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    <T> DeferredResult<List<T>> read(Long after, Supplier<List<T>> query) {
        List<T> initial = query.get();
        var result = new DeferredResult<List<T>>(20_000L, List.of());
        if (after == null || !initial.isEmpty()) {
            result.setResult(initial);
        } else {
            schedule(query, result);
        }
        return result;
    }

    private <T> void schedule(Supplier<List<T>> query, DeferredResult<List<T>> result) {
        executor.schedule(() -> {
            if (result.isSetOrExpired()) return;
            try {
                List<T> messages = query.get();
                if (messages.isEmpty()) schedule(query, result);
                else result.setResult(messages);
            } catch (RuntimeException error) {
                result.setErrorResult(error);
            }
        }, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}