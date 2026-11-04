package cl.gestortareas.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import cl.gestortareas.api.dto.StoredFileResponse;
import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.FilePurpose;
import cl.gestortareas.api.model.StoredFile;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.StoredFileRepository;

@Service
public class FileStorageService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/webp", "application/pdf",
        "text/plain"
    );

    private final StoredFileRepository repository;
    private final AppUserRepository userRepository;

    public FileStorageService(StoredFileRepository repository,
                              AppUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StoredFile store(String email, MultipartFile file,
                            FilePurpose purpose, long maximumSize,
                            Set<String> allowedTypes) {
        validate(file, maximumSize, allowedTypes);
        try {
            return repository.save(new StoredFile(
                findUser(email), safeName(file.getOriginalFilename()),
                file.getContentType(), purpose, file.getBytes()
            ));
        } catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "No fue posible leer el archivo", exception
            );
        }
    }

    @Transactional
    public StoredFileResponse storeAttachment(String email, MultipartFile file) {
        return toResponse(store(
            email, file, FilePurpose.ATTACHMENT, MAX_SIZE, ALLOWED_TYPES
        ));
    }

    @Transactional(readOnly = true)
    public List<StoredFileResponse> list(String email) {
        return repository.findAllByOwnerOrderByCreatedAtDesc(findUser(email))
            .stream().filter(file -> file.getPurpose() == FilePurpose.ATTACHMENT)
            .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StoredFile getOwned(String email, Long id) {
        return repository.findByIdAndOwner(id, findUser(email))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Archivo no encontrado"
            ));
    }

    @Transactional
    public void deleteOwned(String email, Long id) {
        StoredFile file = getOwned(email, id);
        if (file.getPurpose() != FilePurpose.ATTACHMENT) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Este archivo pertenece a otro módulo"
            );
        }
        repository.delete(file);
    }

    @Transactional
    public void delete(StoredFile file) {
        repository.delete(file);
    }

    private void validate(MultipartFile file, long maximumSize,
                          Set<String> allowedTypes) {
        if (file.isEmpty() || file.getSize() > maximumSize) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "El archivo está vacío o supera el tamaño permitido"
            );
        }
        if (file.getContentType() == null ||
            !allowedTypes.contains(file.getContentType())) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de archivo no permitido"
            );
        }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "archivo";
        String normalized = name.replace('\\', '/');
        String result = normalized.substring(normalized.lastIndexOf('/') + 1);
        return result.length() <= 255 ? result : result.substring(result.length() - 255);
    }

    private AppUser findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario no encontrado"
            ));
    }

    private StoredFileResponse toResponse(StoredFile file) {
        return new StoredFileResponse(
            file.getId(), file.getOriginalName(), file.getContentType(),
            file.getSizeBytes(), file.getPurpose().name(), file.getCreatedAt()
        );
    }
}
