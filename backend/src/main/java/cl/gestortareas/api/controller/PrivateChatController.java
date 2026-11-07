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

import cl.gestortareas.api.service.PrivateChatService;
import cl.gestortareas.api.service.PrivateChatService.Conversation;
import cl.gestortareas.api.service.PrivateChatService.Message;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/private-chat")
public class PrivateChatController {
    public record Send(@NotBlank @Size(max = 1000) String content) {}

    private final PrivateChatService chat;
    private final ChatLongPoll polling;

    public PrivateChatController(PrivateChatService chat, ChatLongPoll polling) {
        this.chat = chat;
        this.polling = polling;
    }

    @GetMapping
    public List<Conversation> conversations(Authentication auth) {
        return chat.conversations(auth.getName());
    }

    @GetMapping("/{peerId}")
    public DeferredResult<List<Message>> read(@PathVariable Long peerId,
        @RequestParam(required = false) Long after, Authentication auth) {
        String email = auth.getName();
        return polling.read(after, () -> chat.messages(email, peerId, after));
    }

    @PostMapping("/{peerId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void send(@PathVariable Long peerId, @Valid @RequestBody Send request, Authentication auth) {
        chat.send(auth.getName(), peerId, request.content());
    }
}