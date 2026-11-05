package cl.gestortareas.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(ModuleProperties.class)
public class ModuleConfiguration implements WebMvcConfigurer {

    private final ModuleAvailabilityInterceptor interceptor;

    public ModuleConfiguration(ModuleAvailabilityInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
            .addPathPatterns("/api/tasks/**", "/api/admin/**");
    }
}
