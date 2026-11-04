package cl.gestortareas.api.error;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityErrorWriter {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .findAndRegisterModules();

    public void write(
        HttpServletResponse response,
        int status,
        String message,
        String path
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getOutputStream(),
            ApiErrorResponse.of(
                status,
                ApiExceptionHandler.codeFor(status),
                message,
                path
            )
        );
    }
}
