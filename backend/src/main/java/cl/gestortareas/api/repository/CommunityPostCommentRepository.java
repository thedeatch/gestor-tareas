package cl.gestortareas.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.CommunityPostComment;

public interface CommunityPostCommentRepository extends JpaRepository<CommunityPostComment, Long> {
    @EntityGraph(attributePaths = "author")
    List<CommunityPostComment> findAllByPostIdOrderByCreatedAtAscIdAsc(Long postId);

    Optional<CommunityPostComment> findByPostIdAndId(Long postId, Long id);
}