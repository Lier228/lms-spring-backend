package kz.edu.lms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.edu.lms.exception.ApiError;
import org.springdoc.core.annotations.ParameterObject;

import jakarta.validation.Valid;
import kz.edu.lms.dto.lesson.LessonCreateRequest;
import kz.edu.lms.dto.lesson.LessonResponse;
import kz.edu.lms.dto.lesson.LessonUpdateRequest;
import kz.edu.lms.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Lessons"
        , description = "Manage lessons within a chapter")
@RestController
@RequestMapping("/api/chapters/{chapterId}/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @Operation(summary = "List lessons", description = "Returns a page. Page numbering starts at 0; default size is 10, maximum is 100. Default sort: sortOrder, then id. Use sort=field,asc or sort=field,desc; repeat sort for multiple fields. The chapter must exist.", operationId = "listLesson")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "List lessons", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson or parent chapter not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public Page<LessonResponse> getAll(@Parameter(description = "Parent chapter ID", example = "1", required = true) @PathVariable Long chapterId,
                                       @ParameterObject @PageableDefault(size = 10, sort={"sortOrder", "id"})
                                       Pageable pageable) {
        return lessonService.getAllByChapterId(chapterId, pageable);
    }

    @Operation(summary = "Get a lesson by ID", description = "Returns the requested record. The chapter must exist. The lesson must belong to that chapter.", operationId = "getLesson")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "Get a lesson by ID", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson or parent chapter not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public LessonResponse getById(
            @Parameter(description = "Parent chapter ID", example = "1", required = true) @PathVariable Long chapterId,
            @Parameter(description = "Lesson ID", example = "1", required = true) @PathVariable Long id) {
        return lessonService.getLessonById(chapterId, id);
    }

    @Operation(summary = "Create a lesson", description = "Creates a record from the request body. The chapter must exist.", operationId = "createLesson")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "201", description = "Create a lesson", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson or parent chapter not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@RequestBody @Valid LessonCreateRequest request, @Parameter(description = "Parent chapter ID", example = "1", required = true) @PathVariable Long chapterId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createLesson(chapterId, request));
    }

    @Operation(summary = "Update a lesson", description = "Replaces editable fields. Name and sortOrder are required. Omitted optional fields are cleared. The chapter must exist. The lesson must belong to that chapter.", operationId = "updateLesson")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "Update a lesson", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson or parent chapter not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLesson(@Parameter(description = "Parent chapter ID", example = "1", required = true) @PathVariable Long chapterId, @Parameter(description = "Lesson ID", example = "1", required = true) @PathVariable Long id, @RequestBody @Valid LessonUpdateRequest request) {
        return ResponseEntity.ok(lessonService.updateLesson(chapterId, id, request));
    }

    @Operation(summary = "Delete a lesson", description = "Deletes the record. Returns no response body. The chapter must exist. The lesson must belong to that chapter.", operationId = "deleteLesson")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "204", description = "Delete a lesson", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Lesson or parent chapter not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@Parameter(description = "Parent chapter ID", example = "1", required = true) @PathVariable Long chapterId, @Parameter(description = "Lesson ID", example = "1", required = true) @PathVariable Long id) {
        lessonService.deleteLessonByChapterId(id, chapterId);
        return ResponseEntity.noContent().build();
    }
}
