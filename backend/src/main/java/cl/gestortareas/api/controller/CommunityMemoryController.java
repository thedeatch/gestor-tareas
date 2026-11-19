package cl.gestortareas.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityMemoryService;
import cl.gestortareas.api.service.CommunityMemoryService.Page;

@RestController
@RequestMapping("/api/communities/{groupId}/memories")
public class CommunityMemoryController {
    private final CommunityMemoryService memories;

    public CommunityMemoryController(CommunityMemoryService memories) {
        this.memories = memories;
    }

    @GetMapping
    public Page findAll(@PathVariable Long groupId,
                        @RequestParam(defaultValue = "false") boolean anniversariesOnly,
                        @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return memories.findAll(groupId, auth.getName(), anniversariesOnly, page);
    }
}
