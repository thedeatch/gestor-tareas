package cl.gestortareas.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import cl.gestortareas.api.service.ListPageService;
import cl.gestortareas.api.service.ListPageService.*;
import cl.gestortareas.api.dto.CommunityPostResponse;
import cl.gestortareas.api.model.Task;
import cl.gestortareas.api.service.CommunityAlbumService.Album;
import cl.gestortareas.api.service.CommunityEventService.Event;
import cl.gestortareas.api.service.CommunityTaskService.TaskView;

@RestController
public class ListPageController {
    private final ListPageService pages;
    public ListPageController(ListPageService pages) { this.pages = pages; }
    @GetMapping("/api/communities/{groupId}/tasks/assignees")
    public java.util.List<Assignee> assignees(@PathVariable Long groupId, Authentication auth) {
        return pages.assignees(groupId, auth.getName());
    }
    @GetMapping("/api/communities/{groupId}/posts/page")
    public Page<CommunityPostResponse> posts(@PathVariable Long groupId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Long target, Authentication auth) { return pages.posts(groupId, auth.getName(), page, target); }
    @GetMapping("/api/communities/{groupId}/events/page")
    public Page<Event> events(@PathVariable Long groupId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Long target, Authentication auth) { return pages.events(groupId, auth.getName(), page, target); }
    @GetMapping("/api/communities/{groupId}/albums/page")
    public Page<Album> albums(@PathVariable Long groupId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Long target, Authentication auth) { return pages.albums(groupId, auth.getName(), page, target); }
    @GetMapping("/api/communities/{groupId}/tasks/page")
    public Page<TaskView> groupTasks(@PathVariable Long groupId, @RequestParam(defaultValue = "0") int page,
        @RequestParam(required = false) Long target, @ModelAttribute TaskFilter filter, Authentication auth) {
        return pages.groupTasks(groupId, auth.getName(), page, target, filter);
    }
    @GetMapping("/api/tasks/page")
    public Page<Task> tasks(@RequestParam(defaultValue = "0") int page, @ModelAttribute TaskFilter filter, Authentication auth) {
        return pages.personalTasks(auth.getName(), page, filter);
    }
}
