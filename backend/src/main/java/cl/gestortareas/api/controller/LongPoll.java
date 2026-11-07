package cl.gestortareas.api.controller;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import jakarta.annotation.PreDestroy;

@Component
class LongPoll {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    <T> DeferredResult<List<T>> read(Long after, Supplier<List<T>> query) {
        return await(query, items -> after == null || !items.isEmpty(), List.of());
    }

    <T> DeferredResult<T> await(Supplier<T> query, Predicate<T> changed, T timeout) {
        T initial = query.get();
        var result = new DeferredResult<T>(20_000L, () -> timeout);
        if (changed.test(initial)) result.setResult(initial);
        else schedule(query, changed, result);
        return result;
    }

    private <T> void schedule(Supplier<T> query, Predicate<T> changed, DeferredResult<T> result) {
        executor.schedule(() -> {
            if (result.isSetOrExpired()) return;
            try {
                T value = query.get();
                if (changed.test(value)) result.setResult(value);
                else schedule(query, changed, result);
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