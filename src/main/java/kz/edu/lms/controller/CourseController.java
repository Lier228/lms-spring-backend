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
import kz.edu.lms.dto.course.CourseCreateRequest;
import kz.edu.lms.dto.course.CourseResponse;
import kz.edu.lms.dto.course.CourseUpdateRequest;
import kz.edu.lms.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Courses"
        , description = "Manage courses")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;
    @Operation(summary = "List courses", description = "Returns a page. Page numbering starts at 0; default size is 10, maximum is 100. Default sort: id. Use sort=field,asc or sort=field,desc; repeat sort for multiple fields.", operationId = "listCourse")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "List courses", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/")
    public Page<CourseResponse> getAll(@ParameterObject @PageableDefault(size = 10, sort = "id")Pageable pageable) {
        return courseService.getAllCourses(pageable);
    }

    @Operation(summary = "Get a course by ID", description = "Returns the requested record.", operationId = "getCourse")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "Get a course by ID", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public CourseResponse getById(@Parameter(description = "Course ID", example = "1", required = true) @PathVariable Long id) {
        return courseService.getCourseById(id);
    }

    @Operation(summary = "Create a course", description = "Creates a record from the request body.", operationId = "createCourse")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "201", description = "Create a course", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@RequestBody @Valid CourseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
    }

    @Operation(summary = "Update a course", description = "Replaces editable fields. Name is required. Omitted optional fields are cleared.", operationId = "updateCourse")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "200", description = "Update a course", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(@Parameter(description = "Course ID", example = "1", required = true) @PathVariable Long id, @RequestBody @Valid CourseUpdateRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @Operation(summary = "Delete a course", description = "Deletes the record. Returns no response body. Deletion is rejected while child records exist.", operationId = "deleteCourse")
    @ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid access token", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "204", description = "Delete a course", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body or parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Course not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Cannot delete a record with children", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@Parameter(description = "Course ID", example = "1", required = true) @PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
