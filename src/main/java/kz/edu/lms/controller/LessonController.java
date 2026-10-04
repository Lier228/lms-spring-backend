package kz.edu.lms.controller;

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

@RestController
@RequestMapping("/api/chapters/{chapterId}/lessons")
@RequiredArgsConstructor
public class LessonController {
    private final LessonService lessonService;

    @GetMapping
    public Page<LessonResponse> getAll(@PathVariable Long chapterId,
                                       @PageableDefault(size = 10, sort={"sortOrder", "id"})
                                       Pageable pageable) {
        return lessonService.getAllByChapterId(chapterId, pageable);
    }

    @GetMapping("/{id}")
    public LessonResponse getById(
            @PathVariable Long chapterId,
            @PathVariable Long id) {
        return lessonService.getLessonById(chapterId, id);
    }

    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(@RequestBody @Valid LessonCreateRequest request, @PathVariable Long chapterId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createLesson(chapterId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LessonResponse> updateLesson(@PathVariable Long chapterId, @PathVariable Long id, @RequestBody @Valid LessonUpdateRequest request) {
        return ResponseEntity.ok(lessonService.updateLesson(chapterId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable Long chapterId, @PathVariable Long id) {
        lessonService.deleteLessonByChapterId(id, chapterId);
        return ResponseEntity.noContent().build();
    }
}
