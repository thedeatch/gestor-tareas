package cl.gestortareas.api.controller;

import java.util.List;

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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/chat")
public class CommunityChatController {
    public record Send(@NotBlank @Size(max = 1000) String content) {}

    private final CommunityChatService chat;
    private final ChatLongPoll polling;

    public CommunityChatController(CommunityChatService chat, ChatLongPoll polling) {
        this.chat = chat;
        this.polling = polling;
    }

    @GetMapping
    public DeferredResult<List<Message>> read(@PathVariable Long groupId, @RequestParam(required = false) Long after,
                                              Authentication auth) {
        String email = auth.getName();
        return polling.read(after, () -> chat.messages(groupId, email, after));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void send(@PathVariable Long groupId, @Valid @RequestBody Send request, Authentication auth) {
        chat.send(groupId, auth.getName(), request.content());
    }

}