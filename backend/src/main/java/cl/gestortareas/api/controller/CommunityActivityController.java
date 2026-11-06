package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityActivityService;
import cl.gestortareas.api.service.CommunityActivityService.Activity;

@RestController
@RequestMapping("/api/communities/{groupId}/activity")
public class CommunityActivityController {
    private final CommunityActivityService activity;

    public CommunityActivityController(CommunityActivityService activity) {
        this.activity = activity;
    }

    @GetMapping
    public List<Activity> recent(@PathVariable Long groupId, Authentication auth) {
        return activity.recent(groupId, auth.getName());
    }
}