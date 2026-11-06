package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.UserBlockService;
import cl.gestortareas.api.service.UserBlockService.BlockedUser;
import cl.gestortareas.api.service.UserBlockService.Status;

@RestController
@RequestMapping("/api/users/me/blocks")
public class UserBlockController {
    private final UserBlockService blocks;

    public UserBlockController(UserBlockService blocks) {
        this.blocks = blocks;
    }

    @GetMapping
    public List<BlockedUser> list(Authentication auth) {
        return blocks.list(auth.getName());
    }

    @GetMapping("/{id}")
    public Status status(@PathVariable Long id, Authentication auth) {
        return blocks.status(auth.getName(), id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable Long id, Authentication auth) {
        blocks.set(auth.getName(), id, true);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unblock(@PathVariable Long id, Authentication auth) {
        blocks.set(auth.getName(), id, false);
    }
}