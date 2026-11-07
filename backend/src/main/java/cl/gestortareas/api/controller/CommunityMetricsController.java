package cl.gestortareas.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityMetricsService;
import cl.gestortareas.api.service.CommunityMetricsService.Metrics;
import cl.gestortareas.api.service.CommunityMetricsService.PersonalStats;

@RestController
@RequestMapping("/api/communities/{groupId}/metrics")
public class CommunityMetricsController {
    private final CommunityMetricsService metrics;

    public CommunityMetricsController(CommunityMetricsService metrics) {
        this.metrics = metrics;
    }

    @GetMapping("/mine")
    public PersonalStats mine(@PathVariable Long groupId, Authentication auth) {
        return metrics.mine(groupId, auth.getName());
    }

    @GetMapping
    public Metrics find(@PathVariable Long groupId, @RequestParam(defaultValue = "30") int days,
                        Authentication auth) {
        return metrics.find(groupId, auth.getName(), days);
    }
}