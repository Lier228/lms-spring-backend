package kz.edu.lms.controller;

import kz.edu.lms.dto.attachment.AttachmentResponse;
import kz.edu.lms.exception.GlobalExceptionHandler;
import kz.edu.lms.service.FileService;
import kz.edu.lms.service.AttachmentService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {
    @Mock AttachmentService service;
    private final Authentication auth = new UsernamePasswordAuthenticationToken("admin", "unused");
    private MockMvc mvc;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AttachmentController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void uploadReturns201AndFileDetails() throws Exception {
        var file = new MockMultipartFile("file", "lesson.txt", "text/plain", new byte[]{1, 2});
        when(service.upload(1L, file, auth)).thenReturn(new AttachmentResponse(10L, "lesson.txt", "/download?url=" + id, 1L, java.time.LocalDateTime.now()));
        mvc.perform(multipart("/upload").file(file).param("lessonId", "1").principal(auth)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("lesson.txt"))
                .andExpect(jsonPath("$.lessonId").value(1));
    }

    @Test
    void missingFileAndInvalidIdReturn400() throws Exception {
        mvc.perform(multipart("/upload").param("lessonId", "1").principal(auth)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(get("/download").param("url", "not-a-uuid").principal(auth)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void downloadStreamsBytesAsAttachmentAndClosesInput() throws Exception {
        byte[] bytes = {0, 1, 2, -1};
        var input = spy(new ByteArrayInputStream(bytes));
        when(service.download(id, auth)).thenReturn(new FileService.Download(new InputStreamResource(input), "Урок.pdf", bytes.length));
        var response = mvc.perform(get("/download").param("url", id.toString()).principal(auth)).andExpect(status().isOk())
                .andExpect(content().bytes(bytes)).andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().longValue("Content-Length", bytes.length))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff")).andReturn().getResponse();
        var disposition = ContentDisposition.parse(response.getHeader("Content-Disposition"));
        assertTrue(disposition.isAttachment());
        assertEquals("Урок.pdf", disposition.getFilename());
        verify(input).close();
    }

    @Test
    void returnsSafeErrorsForMissingFileAndUnavailableStorage() throws Exception {
        when(service.download(id, auth)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        mvc.perform(get("/download").param("url", id.toString()).principal(auth)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("File not found"));
        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "private storage details")).when(service).download(id, auth);
        mvc.perform(get("/download").param("url", id.toString()).principal(auth)).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
    @Test
    void uploadRequiresLessonIdAndDownloadRequiresUrl() throws Exception {
        mvc.perform(multipart("/upload").file(new MockMultipartFile("file", new byte[]{1})).principal(auth))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/download").principal(auth)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

}
