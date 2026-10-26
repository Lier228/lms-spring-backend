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
import kz.edu.lms.dto.chapter.ChapterCreateRequest;
import kz.edu.lms.dto.chapter.ChapterResponse;
import kz.edu.lms.dto.chapter.ChapterUpdateRequest;
import kz.edu.lms.service.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Chapters"
        , description = "Manage chapters within a course")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/chapters")
public class ChapterController {
    private final ChapterService chapterService;

    @Operation(summary = "List chapters", description = "Returns a page. Page numbering starts at 0; default size is 10, maximum is 100. Default sort: sortOrder, then id. Use sort=field,asc or sort=field,desc; repeat sort for multiple fields. The course must exist.", operationId = "listChapter")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "List chapters", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Chapter or parent course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public Page<ChapterResponse> getAll(@Parameter(description = "Parent course ID", example = "1", required = true) @PathVariable Long courseId,
                                        @ParameterObject @PageableDefault(size = 10, sort = {"sortOrder", "id"})
                                        Pageable pageable) {
        return chapterService.getAllByCourseId(courseId, pageable);
    }

    @Operation(summary = "Get a chapter by ID", description = "Returns the requested record. The course must exist. The chapter must belong to that course.", operationId = "getChapter")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "Get a chapter by ID", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Chapter or parent course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ChapterResponse getById(
            @Parameter(description = "Parent course ID", example = "1", required = true) @PathVariable Long courseId,
            @Parameter(description = "Chapter ID", example = "1", required = true) @PathVariable Long id) {
        return chapterService.getChapterById(courseId, id);
    }

    @Operation(summary = "Create a chapter", description = "Creates a record from the request body. The course must exist.", operationId = "createChapter")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "201", description = "Create a chapter", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Chapter or parent course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ChapterResponse> createChapter(@RequestBody @Valid ChapterCreateRequest request, @Parameter(description = "Parent course ID", example = "1", required = true) @PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chapterService.createChapter(courseId, request));
    }

    @Operation(summary = "Update a chapter", description = "Replaces editable fields. Name and sortOrder are required. Omitted optional fields are cleared. The course must exist. The chapter must belong to that course.", operationId = "updateChapter")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "Update a chapter", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Chapter or parent course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ChapterResponse> updateChapter(@Parameter(description = "Parent course ID", example = "1", required = true) @PathVariable Long courseId, @Parameter(description = "Chapter ID", example = "1", required = true) @PathVariable Long id, @RequestBody @Valid ChapterUpdateRequest request) {
        return ResponseEntity.ok(chapterService.updateChapter(courseId, id, request));
    }

    @Operation(summary = "Delete a chapter", description = "Deletes the record. Returns no response body. The course must exist. The chapter must belong to that course. Deletion is rejected while child records exist.", operationId = "deleteChapter")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "204", description = "Delete a chapter", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Chapter or parent course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Cannot delete a record with children", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@Parameter(description = "Parent course ID", example = "1", required = true) @PathVariable Long courseId, @Parameter(description = "Chapter ID", example = "1", required = true) @PathVariable Long id) {
        chapterService.deleteChapterByCourseId(id, courseId);
        return ResponseEntity.noContent().build();
    }
}
