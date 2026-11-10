package kz.edu.lms.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import okhttp3.Headers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {
    @Mock MinioClient client;
    private FileService service;
    private final UUID id = UUID.randomUUID();
    private final byte[] bytes = {0, 1, 2, -1};

    @BeforeEach
    void setUp() { service = new FileService(client, "lms-files"); }

    @Test
    void uploadsWithUniqueIdsAndPreservesSafeUnicodeFilename() throws Exception {
        var file = new MockMultipartFile("file", "C:\\fakepath\\Урок.pdf", "application/pdf", bytes);
        var first = service.upload(file);
        var second = service.upload(file);
        assertNotEquals(first.id(), second.id());
        assertEquals("Урок.pdf", first.filename());
        assertEquals(bytes.length, first.size());
        var captor = org.mockito.ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(client, times(2)).putObject(captor.capture());
        var args = captor.getAllValues().getFirst();
        assertEquals("lms-files", args.bucket());
        assertEquals(first.id().toString(), args.object());
        assertEquals(bytes.length, args.objectSize());
        assertEquals("application/octet-stream", args.contentType());
        String encoded = Base64.getEncoder().encodeToString("Урок.pdf".getBytes(StandardCharsets.UTF_8));
        assertTrue(args.genHeaders().get("x-amz-meta-filename").contains(encoded));
    }

    @Test
    void rejectsEmptyFileBeforeCallingStorage() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", new byte[0])));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(client);
    }

    @Test
    void storageFailureReturns502WithoutLeakingDetails() throws Exception {
        when(client.putObject(any(PutObjectArgs.class))).thenThrow(new IOException("secret storage credentials"));
        var error = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", bytes)));
        assertEquals(502, error.getStatusCode().value());
        assertFalse(error.getMessage().contains("secret"));
        assertNull(error.getCause());
    }

    @Test
    void downloadsOriginalBytesAndFilename() throws Exception {
        String filename = "Урок.pdf";
        var headers = new Headers.Builder().add("Content-Length", "4")
                .add("x-amz-meta-filename", Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8))).build();
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(
                new GetObjectResponse(headers, "lms-files", null, id.toString(), new ByteArrayInputStream(bytes)));
        var result = service.download(id);
        assertEquals(filename, result.filename());
        assertEquals(4, result.size());
        try (var input = result.resource().getInputStream()) { assertArrayEquals(bytes, input.readAllBytes()); }
        var captor = org.mockito.ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(client).getObject(captor.capture());
        assertEquals(id.toString(), captor.getValue().object());
        assertEquals("lms-files", captor.getValue().bucket());
    }

    @Test
    void onlyMissingObjectReturns404() throws Exception {
        var error = mock(ErrorResponseException.class);
        var response = mock(ErrorResponse.class);
        when(error.errorResponse()).thenReturn(response);
        when(client.getObject(any(GetObjectArgs.class))).thenThrow(error);
        when(response.code()).thenReturn("NoSuchKey");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.download(id)).getStatusCode().value());
        when(response.code()).thenReturn("NoSuchBucket");
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.download(id)).getStatusCode().value());
    }

    @Test
    void closesStreamWhenStorageReturnsInvalidMetadata() throws Exception {
        var stream = spy(new ByteArrayInputStream(bytes));
        var headers = new Headers.Builder().add("Content-Length", "invalid").build();
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(
                new GetObjectResponse(headers, "lms-files", null, id.toString(), stream));
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.download(id)).getStatusCode().value());
        verify(stream).close();
    }
    @Test
    void deletesOnlyRequestedObjectDuringRollback() throws Exception {
        service.delete(id);
        var captor = org.mockito.ArgumentCaptor.forClass(RemoveObjectArgs.class);
        verify(client).removeObject(captor.capture());
        assertEquals("lms-files", captor.getValue().bucket());
        assertEquals(id.toString(), captor.getValue().object());
    }

    @Test
    void deleteStorageFailureReturns502() throws Exception {
        doThrow(new IOException("Storage details")).when(client).removeObject(any(RemoveObjectArgs.class));
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.delete(id)).getStatusCode().value());
    }

}
