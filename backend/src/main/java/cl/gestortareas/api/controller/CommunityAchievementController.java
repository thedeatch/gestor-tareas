package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.gestortareas.api.service.CommunityAchievementService;
import cl.gestortareas.api.service.CommunityAchievementService.Achievement;

@RestController
@RequestMapping("/api/communities/{groupId}/achievements")
public class CommunityAchievementController {
    private final CommunityAchievementService achievements;

    public CommunityAchievementController(CommunityAchievementService achievements) {
        this.achievements = achievements;
    }

    @GetMapping("/mine")
    public List<Achievement> mine(@PathVariable Long groupId, Authentication auth) {
        return achievements.mine(groupId, auth.getName());
    }
}