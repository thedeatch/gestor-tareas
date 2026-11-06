package cl.gestortareas.api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import cl.gestortareas.api.dto.CommunityDetailResponse;
import cl.gestortareas.api.dto.CommunitySummaryResponse;
import cl.gestortareas.api.dto.CreateCommunityRequest;
import cl.gestortareas.api.service.CommunityService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Grupos", description = "Grupos internos de la comunidad y sus membresías")
@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public List<CommunitySummaryResponse> findMine(Authentication authentication) {
        return communityService.findMine(authentication.getName());
    }

    @GetMapping("/{id}")
    public CommunityDetailResponse findById(@PathVariable Long id,
                                            Authentication authentication) {
        return communityService.findById(id, authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommunitySummaryResponse create(
        @Valid @RequestBody CreateCommunityRequest request,
        Authentication authentication
    ) {
        return communityService.create(request, authentication.getName());
    }
}
