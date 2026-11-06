package cl.gestortareas.api.controller;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import cl.gestortareas.api.service.CommunityChatService;
import cl.gestortareas.api.service.CommunityChatService.Message;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/chat")
public class CommunityChatController {
    public record Send(@NotBlank @Size(max = 1000) String content) {}

    private final CommunityChatService chat;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public CommunityChatController(CommunityChatService chat) {
        this.chat = chat;
    }

    @GetMapping
    public DeferredResult<List<Message>> read(@PathVariable Long groupId, @RequestParam(required = false) Long after,
                                              Authentication auth) {
        String email = auth.getName();
        var initial = chat.messages(groupId, email, after);
        var result = new DeferredResult<List<Message>>(20_000L, List.of());
        if (after == null || !initial.isEmpty()) {
            result.setResult(initial);
        } else {
            scheduleRead(groupId, email, after, result);
        }
        return result;
    }

    private void scheduleRead(Long groupId, String email, Long after, DeferredResult<List<Message>> result) {
        executor.schedule(() -> {
            if (result.isSetOrExpired()) return;
            try {
                var messages = chat.messages(groupId, email, after);
                if (messages.isEmpty()) {
                    scheduleRead(groupId, email, after, result);
                } else {
                    result.setResult(messages);
                }
            } catch (RuntimeException error) {
                result.setErrorResult(error);
            }
        }, 1, TimeUnit.SECONDS);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void send(@PathVariable Long groupId, @Valid @RequestBody Send request, Authentication auth) {
        chat.send(groupId, auth.getName(), request.content());
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}