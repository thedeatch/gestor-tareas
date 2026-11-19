package cl.gestortareas.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import cl.gestortareas.api.service.CommunitySearchService;
import cl.gestortareas.api.service.CommunitySearchService.Filter;
import cl.gestortareas.api.service.CommunitySearchService.Page;

@RestController
@RequestMapping("/api/communities/{groupId}/search")
public class CommunitySearchController {
    private final CommunitySearchService search;

    public CommunitySearchController(CommunitySearchService search) {
        this.search = search;
    }

    @GetMapping
    public Page search(@PathVariable Long groupId, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "ALL") Filter kind,
                       @RequestParam(defaultValue = "0") int page, Authentication auth) {
        return search.search(groupId, auth.getName(), q, kind, page);
    }
}
