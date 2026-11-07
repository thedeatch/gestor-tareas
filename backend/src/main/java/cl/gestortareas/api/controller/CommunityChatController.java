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
import cl.gestortareas.api.service.CommunityChatService.Channel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/chat")
public class CommunityChatController {
    public record Send(@NotBlank @Size(max = 1000) String content) {}

    public record CreateChannel(@NotBlank @Size(max = 60) String name) {}

    private final CommunityChatService chat;
    private final LongPoll polling;

    public CommunityChatController(CommunityChatService chat, LongPoll polling) {
        this.chat = chat;
        this.polling = polling;
    }

    @GetMapping("/channels")
    public List<Channel> channels(@PathVariable Long groupId, Authentication auth) {
        return chat.channels(groupId, auth.getName());
    }

    @PostMapping("/channels")
    @ResponseStatus(HttpStatus.CREATED)
    public void createChannel(@PathVariable Long groupId, @Valid @RequestBody CreateChannel request,
                              Authentication auth) {
        chat.createChannel(groupId, auth.getName(), request.name());
    }

    @GetMapping({"", "/channels/{channelId}"})
    public DeferredResult<List<Message>> read(@PathVariable Long groupId,
        @PathVariable(required = false) Long channelId, @RequestParam(required = false) Long after,
        Authentication auth) {
        String email = auth.getName();
        return polling.read(after, () -> chat.messages(groupId, channelId, email, after));
    }

    @PostMapping({"", "/channels/{channelId}"})
    @ResponseStatus(HttpStatus.CREATED)
    public void send(@PathVariable Long groupId, @PathVariable(required = false) Long channelId,
                     @Valid @RequestBody Send request, Authentication auth) {
        chat.send(groupId, channelId, auth.getName(), request.content());
    }

}