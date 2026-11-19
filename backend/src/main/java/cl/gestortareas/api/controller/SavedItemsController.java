package cl.gestortareas.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.SavedItemsService;
import cl.gestortareas.api.service.SavedItemsService.Filter;
import cl.gestortareas.api.service.SavedItemsService.Kind;
import cl.gestortareas.api.service.SavedItemsService.Page;
import cl.gestortareas.api.service.SavedItemsService.State;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/saved-items")
public class SavedItemsController {
    public record SaveRequest(@NotNull Long groupId) {}
    private final SavedItemsService saved;

    public SavedItemsController(SavedItemsService saved) {
        this.saved = saved;
    }

    @GetMapping
    public Page findAll(@RequestParam(defaultValue = "ALL") Filter kind,
                        @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return saved.findAll(auth.getName(), kind, page);
    }

    @GetMapping("/state")
    public State state(@RequestParam Long groupId, Authentication auth) {
        return saved.state(groupId, auth.getName());
    }

    @PutMapping("/{kind}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable Kind kind, @PathVariable Long targetId,
                     @Valid @RequestBody SaveRequest request, Authentication auth) {
        saved.save(auth.getName(), request.groupId(), kind, targetId);
    }

    @DeleteMapping("/{kind}/{targetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Kind kind, @PathVariable Long targetId, Authentication auth) {
        saved.remove(auth.getName(), kind, targetId);
    }
}
