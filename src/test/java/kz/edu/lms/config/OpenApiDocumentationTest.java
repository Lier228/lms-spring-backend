package kz.edu.lms.config;

import com.jayway.jsonpath.JsonPath;
import kz.edu.lms.controller.*;
import kz.edu.lms.exception.GlobalExceptionHandler;
import kz.edu.lms.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = OpenApiDocumentationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.docker.compose.enabled=false", "springdoc.override-with-generic-response=false"})
class OpenApiDocumentationTest {
    @Autowired Environment environment;

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration"
    })
    @Import({CourseController.class, ChapterController.class, LessonController.class,
            GlobalExceptionHandler.class, OpenApiConfig.class})
    static class TestConfig {
        @Bean CourseService courseService() { return mock(CourseService.class); }
        @Bean ChapterService chapterService() { return mock(ChapterService.class); }
        @Bean LessonService lessonService() { return mock(LessonService.class); }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(
                "http://localhost:" + environment.getProperty("local.server.port") + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void documentsAllOperationsAndErrorSchemas() throws Exception {
        var response = get("/v3/api-docs");
        assertEquals(200, response.statusCode());
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(response.body(), "$.paths");
        int operations = 0;
        for (var path : paths.values()) {
            for (var entry : path.entrySet()) {
                if (!java.util.Set.of("get", "post", "put", "delete").contains(entry.getKey())) continue;
                operations++;
                assertNotNull(entry.getValue().get("summary"));
                Map<?, ?> responses = (Map<?, ?>) entry.getValue().get("responses");
                assertTrue(responses.containsKey("500"));
                String success = entry.getKey().equals("post") ? "201" : entry.getKey().equals("delete") ? "204" : "200";
                assertTrue(responses.containsKey(success));
            }
        }
        assertEquals(15, operations);
        assertTrue(paths.containsKey("/api/chapters/{chapterId}/lessons/{id}"));
        assertTrue(response.body().contains("#/components/schemas/ApiError"));
        assertTrue(response.body().contains("sortOrder"));
        assertTrue(response.body().contains("\"page\""));
        assertTrue(response.body().contains("\"size\""));
        assertTrue(response.body().contains("\"sort\""));
    }

    @Test
    void swaggerUiIsAvailable() throws Exception {
        var response = get("/swagger-ui/index.html");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Swagger UI"));
    }
}
