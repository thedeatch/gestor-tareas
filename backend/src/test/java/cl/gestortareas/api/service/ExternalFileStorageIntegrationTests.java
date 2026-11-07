package cl.gestortareas.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.aryEq;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import cl.gestortareas.api.model.AppUser;
import cl.gestortareas.api.model.FilePurpose;
import cl.gestortareas.api.model.StoredFile;
import cl.gestortareas.api.repository.AppUserRepository;
import cl.gestortareas.api.repository.StoredFileRepository;

@SpringBootTest
class ExternalFileStorageIntegrationTests {
    @Autowired FileStorageService storage;
    @Autowired AppUserRepository users;
    @Autowired StoredFileRepository files;
    @Autowired TransactionTemplate transactions;
    @MockitoBean S3FileStorage objects;

    @Test
    void shouldStoreNewBytesExternallyAndReadLegacyFiles() {
        String email = "external-storage@gestor.cl";
        var user = users.save(new AppUser("Archivos externos", email, "hash"));
        byte[] data = {1, 2, 3};
        var legacy = files.save(new StoredFile(user, "anterior.txt", "text/plain", FilePurpose.ATTACHMENT, data));
        assertArrayEquals(data, storage.readBytes(storage.getOwned(email, legacy.getId())));

        var upload = new MockMultipartFile("file", "nuevo.txt", "text/plain", data);
        var response = storage.storeAttachment(email, upload);
        var stored = storage.getOwned(email, response.id());
        assertNull(stored.getData());
        assertEquals(3, stored.getSizeBytes());
        assertNotNull(stored.getObjectKey());
        verify(objects).put(eq(stored.getObjectKey()), eq("text/plain"), aryEq(data));

        when(objects.read(stored.getObjectKey())).thenReturn(data);
        assertArrayEquals(data, storage.readBytes(stored));
        transactions.executeWithoutResult(transaction -> {
            storage.deleteOwned(email, stored.getId());
            verify(objects, never()).delete(stored.getObjectKey());
            transaction.setRollbackOnly();
        });
        assertTrue(files.existsById(stored.getId()));
        storage.deleteOwned(email, stored.getId());
        verify(objects).delete(stored.getObjectKey());
        assertFalse(files.existsById(stored.getId()));

        clearInvocations(objects);
        transactions.executeWithoutResult(transaction -> {
            storage.storeAttachment(email, upload);
            transaction.setRollbackOnly();
        });
        var rolledBackKey = ArgumentCaptor.forClass(String.class);
        verify(objects).put(rolledBackKey.capture(), eq("text/plain"), aryEq(data));
        verify(objects).delete(rolledBackKey.getValue());
        storage.deleteOwned(email, legacy.getId());
        users.delete(user);
    }
}