package kz.edu.lms.controller;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses/{courseId}/chapters")
public class ChapterController {
    private final ChapterService chapterService;

    @GetMapping
    public Page<ChapterResponse> getAll(@PathVariable Long courseId,
                                        @PageableDefault(size = 10, sort = {"sortOrder", "id"})
                                        Pageable pageable) {
        return chapterService.getAllByCourseId(courseId, pageable);
    }

    @GetMapping("/{id}")
    public ChapterResponse getById(
            @PathVariable Long courseId,
            @PathVariable Long id) {
        return chapterService.getChapterById(courseId, id);
    }

    @PostMapping
    public ResponseEntity<ChapterResponse> createChapter(@RequestBody @Valid ChapterCreateRequest request, @PathVariable Long courseId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chapterService.createChapter(courseId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChapterResponse> updateChapter(@PathVariable Long courseId, @PathVariable Long id, @RequestBody @Valid ChapterUpdateRequest request) {
        return ResponseEntity.ok(chapterService.updateChapter(courseId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long courseId, @PathVariable Long id) {
        chapterService.deleteChapterByCourseId(id, courseId);
        return ResponseEntity.noContent().build();
    }
}
