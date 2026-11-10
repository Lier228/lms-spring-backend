package kz.edu.lms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.edu.lms.dto.attachment.AttachmentResponse;
import kz.edu.lms.exception.ApiError;
import kz.edu.lms.service.AttachmentService;
import org.springframework.security.core.Authentication;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag(name = "Attachments")
@RestController
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    @Operation(summary = "Upload an attachment", description = "ROLE_ADMIN or ROLE_TEACHER. Attach a multipart file to the lesson. Maximum size is 50 MB.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "File uploaded", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Missing or empty file", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "ROLE_ADMIN or ROLE_TEACHER required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson or course not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "413", description = "File too large", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Storage unavailable", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(@RequestParam Long lessonId,
                                                      @RequestPart("file") MultipartFile file,
                                                      Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.upload(lessonId, file, authentication));
    }

    @Operation(summary = "Download an attachment", description = "Checks access to the lesson course before reading storage. Currently ROLE_USER, ROLE_TEACHER and ROLE_ADMIN can read every course. Use the URL returned by upload.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "File content", content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "400", description = "Invalid file ID", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "File not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "502", description = "Storage unavailable", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam UUID url, Authentication authentication) {
        var file = attachmentService.download(url, authentication);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.size()).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.resource());
    }
}
