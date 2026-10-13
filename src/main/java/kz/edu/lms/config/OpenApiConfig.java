package kz.edu.lms.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "LMS API",
        version = "1.0",
        description = "API for courses, chapters and lessons. Authentication is not enabled at this stage."
))
public class OpenApiConfig {
}
