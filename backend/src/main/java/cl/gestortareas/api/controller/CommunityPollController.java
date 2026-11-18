package cl.gestortareas.api.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityPollService;
import cl.gestortareas.api.service.CommunityPollService.Poll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/communities/{groupId}/polls")
public class CommunityPollController {
    public record CreatePoll(
        @NotBlank @Size(max = 300) String question,
        @NotNull @Size(min = 2, max = 10) List<@NotBlank @Size(max = 200) String> options,
        OffsetDateTime closesAt
    ) {}
    public record Vote(@NotNull Long optionId) {}

    private final CommunityPollService polls;

    public CommunityPollController(CommunityPollService polls) {
        this.polls = polls;
    }

    @GetMapping
    public List<Poll> findAll(@PathVariable Long groupId, Authentication auth) {
        return polls.findAll(groupId, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Poll create(@PathVariable Long groupId, @Valid @RequestBody CreatePoll request,
                       Authentication auth) {
        return polls.create(groupId, auth.getName(), request.question(), request.options(), request.closesAt());
    }

    @PutMapping("/{pollId}/vote")
    public Poll vote(@PathVariable Long groupId, @PathVariable Long pollId,
                     @Valid @RequestBody Vote request, Authentication auth) {
        return polls.vote(groupId, pollId, auth.getName(), request.optionId());
    }

    @PutMapping("/{pollId}/close")
    public Poll close(@PathVariable Long groupId, @PathVariable Long pollId, Authentication auth) {
        return polls.close(groupId, pollId, auth.getName());
    }
}
