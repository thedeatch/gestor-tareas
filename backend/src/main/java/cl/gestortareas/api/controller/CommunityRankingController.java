package cl.gestortareas.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityRankingService;
import cl.gestortareas.api.service.CommunityRankingService.Ranking;

@RestController
@RequestMapping("/api/communities/{groupId}/ranking")
public class CommunityRankingController {
    private final CommunityRankingService ranking;

    public CommunityRankingController(CommunityRankingService ranking) {
        this.ranking = ranking;
    }

    @GetMapping
    public Ranking find(@PathVariable Long groupId, @RequestParam(required = false) String season,
                        Authentication auth) {
        return ranking.find(groupId, auth.getName(), season);
    }
}