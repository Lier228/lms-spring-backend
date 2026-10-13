package kz.edu.lms.controller;

import kz.edu.lms.dto.lesson.*;
import kz.edu.lms.service.LessonService;
import kz.edu.lms.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {
    @Mock LessonService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private final LessonResponse response = new LessonResponse(1L, "New name", "Description", "Content", 2, null, null);
    private static final String BODY = "{\"name\":\"New name\",\"description\":\"Description\",\"content\":\"Content\",\"sortOrder\":2}";

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(100);
        mvc = MockMvcBuilders.standaloneSetup(new LessonController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(pageableResolver).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() { validator.close(); }

    @Test
    void listsWithRequestedPagination() throws Exception {
        when(service.getAllByChapterId(eq(10L), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(response), invocation.getArgument(1), 11));
        mvc.perform(get("/api/chapters/10/lessons").param("page", "1").param("size", "10").param("sort", "name,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(11));
        verify(service).getAllByChapterId(eq(10L), eq(PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "name"))));
    }

    @Test
    void getsRecordUsingPathIds() throws Exception {
        when(service.getLessonById(10L, 1L)).thenReturn(response);
        mvc.perform(get("/api/chapters/10/lessons/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));
        verify(service).getLessonById(10L, 1L);
    }

    @Test
    void createsAndReturns201() throws Exception {
        var request = new LessonCreateRequest("New name", "Description", "Content", 2);
        when(service.createLesson(10L, request)).thenReturn(response);
        mvc.perform(post("/api/chapters/10/lessons").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
        verify(service).createLesson(10L, request);
    }

    @Test
    void updatesAndReturns200() throws Exception {
        var request = new LessonUpdateRequest("New name", "Description", "Content", 2);
        when(service.updateLesson(10L, 1L, request)).thenReturn(response);
        mvc.perform(put("/api/chapters/10/lessons/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New name"));
        verify(service).updateLesson(10L, 1L, request);
    }

    @Test
    void deletesAndReturnsEmpty204() throws Exception {
        mvc.perform(delete("/api/chapters/10/lessons/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).deleteLessonByChapterId(1L, 10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsBlankNameBeforeCallingService(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/chapters/10/lessons" + (method.equals("PUT") ? "/1" : ""))
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("New name", " ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/chapters/10/lessons").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingRecordReturns404() throws Exception {
        when(service.getLessonById(10L, 1L)).thenThrow(new IllegalArgumentException("Lesson not found"));
        mvc.perform(get("/api/chapters/10/lessons/1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lesson not found"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unexpectedFailureDoesNotLeakDetails() throws Exception {
        when(service.getLessonById(10L, 1L)).thenThrow(new RuntimeException("private SQL details"));
        mvc.perform(get("/api/chapters/10/lessons/1")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "null"})
    void rejectsInvalidSortOrder(String value) throws Exception {
        mvc.perform(post("/api/chapters/10/lessons").contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"sortOrder\":2", "\"sortOrder\":" + value)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
