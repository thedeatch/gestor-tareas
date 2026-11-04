package cl.gestortareas.api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    List<StoredFile> findAllByOwnerOrderByCreatedAtDesc(AppUser owner);
    Optional<StoredFile> findByIdAndOwner(Long id, AppUser owner);
}

