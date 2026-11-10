package kz.edu.lms.service;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import kz.edu.lms.dto.file.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class FileService {
    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private final MinioClient minioClient;
    private final String bucket;

    public FileService(MinioClient minioClient, @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public FileResponse upload(MultipartFile file) {
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        UUID id = UUID.randomUUID();
        String filename = safeFilename(file.getOriginalFilename());
        String encodedName = Base64.getEncoder().encodeToString(filename.getBytes(StandardCharsets.UTF_8));
        log.info("Uploading file");
        try (var input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(id.toString())
                    .stream(input, file.getSize(), -1)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .userMetadata(Map.of("filename", encodedName)).build());
        } catch (Exception exception) {
            throw storageError(exception);
        }
        log.debug("Uploaded file: id={}, size={}", id, file.getSize());
        return new FileResponse(id, filename, file.getSize());
    }

    public Download download(UUID id) {
        log.info("Downloading file: {}", id);
        GetObjectResponse object = null;
        try {
            object = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(id.toString()).build());
            long size = Long.parseLong(object.headers().get("Content-Length"));
            String encodedName = object.headers().get("x-amz-meta-filename");
            String filename = encodedName == null ? id.toString()
                    : safeFilename(new String(Base64.getDecoder().decode(encodedName), StandardCharsets.UTF_8));
            return new Download(new InputStreamResource(object), filename, size);
        } catch (Exception exception) {
            if (object != null) {
                try { object.close(); } catch (IOException ignored) { }
            }
            if (exception instanceof ErrorResponseException error && "NoSuchKey".equals(error.errorResponse().code())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            throw storageError(exception);
        }
    }

    public void delete(UUID id) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(id.toString()).build());
        } catch (Exception exception) {
            throw storageError(exception);
        }
    }

    private String safeFilename(String name) {
        if (name == null) return "file";
        String filename = StringUtils.getFilename(name.replace('\\', '/'));
        if (filename == null) return "file";
        filename = filename.replaceAll("[\\p{Cntrl}]", "");
        if (!StringUtils.hasText(filename) || filename.equals(".") || filename.equals("..")) return "file";
        return filename.length() > 200 ? filename.substring(0, 200) : filename;
    }

    private ResponseStatusException storageError(Exception exception) {
        log.error("File storage request failed: {}", exception.getClass().getSimpleName());
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "File storage unavailable");
    }

    public record Download(InputStreamResource resource, String filename, long size) {
    }
}
