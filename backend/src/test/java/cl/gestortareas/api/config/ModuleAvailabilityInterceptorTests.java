package cl.gestortareas.api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class ModuleAvailabilityInterceptorTests {

    @Test
    void shouldHidePrivateChatWhenMessagingIsDisabled() {
        var modules = new ModuleProperties(true, true, false, true, true);
        var interceptor = new ModuleAvailabilityInterceptor(modules);
        var request = new MockHttpServletRequest("GET", "/api/private-chat");

        var error = assertThrows(ResponseStatusException.class,
            () -> interceptor.preHandle(
                request,
                new MockHttpServletResponse(),
                new Object()
            ));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void shouldAllowPrivateChatWhenMessagingIsEnabled() {
        var modules = new ModuleProperties(true, true, true, true, true);
        var interceptor = new ModuleAvailabilityInterceptor(modules);
        var request = new MockHttpServletRequest("GET", "/api/private-chat");

        assertDoesNotThrow(() -> interceptor.preHandle(
            request,
            new MockHttpServletResponse(),
            new Object()
        ));
    }
}
