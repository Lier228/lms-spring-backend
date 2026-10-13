package kz.edu.lms.controller;

import kz.edu.lms.dto.chapter.*;
import kz.edu.lms.service.ChapterService;
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
class ChapterControllerTest {
    @Mock ChapterService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private final ChapterResponse response = new ChapterResponse(1L, "New name", "Description", 2, null, null);
    private static final String BODY = "{\"name\":\"New name\",\"description\":\"Description\",\"sortOrder\":2}";

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var pageableResolver = new PageableHandlerMethodArgumentResolver();
        pageableResolver.setMaxPageSize(100);
        mvc = MockMvcBuilders.standaloneSetup(new ChapterController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(pageableResolver).setValidator(validator).build();
    }

    @AfterEach
    void tearDown() { validator.close(); }

    @Test
    void listsWithRequestedPagination() throws Exception {
        when(service.getAllByCourseId(eq(10L), any(Pageable.class))).thenAnswer(invocation ->
                new PageImpl<>(List.of(response), invocation.getArgument(1), 11));
        mvc.perform(get("/api/courses/10/chapters").param("page", "1").param("size", "10").param("sort", "name,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(11));
        verify(service).getAllByCourseId(eq(10L), eq(PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "name"))));
    }

    @Test
    void getsRecordUsingPathIds() throws Exception {
        when(service.getChapterById(10L, 1L)).thenReturn(response);
        mvc.perform(get("/api/courses/10/chapters/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));
        verify(service).getChapterById(10L, 1L);
    }

    @Test
    void createsAndReturns201() throws Exception {
        var request = new ChapterCreateRequest("New name", 2, "Description");
        when(service.createChapter(10L, request)).thenReturn(response);
        mvc.perform(post("/api/courses/10/chapters").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
        verify(service).createChapter(10L, request);
    }

    @Test
    void updatesAndReturns200() throws Exception {
        var request = new ChapterUpdateRequest("New name", 2, "Description");
        when(service.updateChapter(10L, 1L, request)).thenReturn(response);
        mvc.perform(put("/api/courses/10/chapters/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New name"));
        verify(service).updateChapter(10L, 1L, request);
    }

    @Test
    void deletesAndReturnsEmpty204() throws Exception {
        mvc.perform(delete("/api/courses/10/chapters/1")).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).deleteChapterByCourseId(1L, 10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void rejectsBlankNameBeforeCallingService(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), "/api/courses/10/chapters" + (method.equals("PUT") ? "/1" : ""))
                .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("New name", " ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.trace").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/courses/10/chapters").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void missingRecordReturns404() throws Exception {
        when(service.getChapterById(10L, 1L)).thenThrow(new IllegalArgumentException("Chapter not found"));
        mvc.perform(get("/api/courses/10/chapters/1")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Chapter not found"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void unexpectedFailureDoesNotLeakDetails() throws Exception {
        when(service.getChapterById(10L, 1L)).thenThrow(new RuntimeException("private SQL details"));
        mvc.perform(get("/api/courses/10/chapters/1")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void deleteConflictReturns409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Record contains children"))
                .when(service).deleteChapterByCourseId(1L, 10L);
        mvc.perform(delete("/api/courses/10/chapters/1")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Record contains children"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "null"})
    void rejectsInvalidSortOrder(String value) throws Exception {
        mvc.perform(post("/api/courses/10/chapters").contentType(MediaType.APPLICATION_JSON)
                .content(BODY.replace("\"sortOrder\":2", "\"sortOrder\":" + value)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
