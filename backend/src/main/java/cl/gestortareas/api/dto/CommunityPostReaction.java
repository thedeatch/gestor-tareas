package cl.gestortareas.api.dto;

public record CommunityPostReaction(Type type, long count, boolean selected) {
    public enum Type {
        LIKE, LOVE, CELEBRATE
    }
}