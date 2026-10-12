package kz.edu.lms.service;

import kz.edu.lms.dto.chapter.ChapterCreateRequest;
import kz.edu.lms.dto.chapter.ChapterResponse;
import kz.edu.lms.dto.chapter.ChapterUpdateRequest;
import kz.edu.lms.entity.Chapter;
import kz.edu.lms.entity.Course;
import kz.edu.lms.mapper.ChapterMapper;
import kz.edu.lms.repository.ChapterRepository;
import kz.edu.lms.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {
    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final ChapterMapper chapterMapper;

    @Transactional(readOnly = true)
    public Page<ChapterResponse> getAllChapters(Pageable pageable) {
        return chapterRepository.findAll(pageable).map(chapterMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ChapterResponse> getAllByCourseId(Long courseId, Pageable pageable) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found");
        }

        return chapterRepository.findAllByCourseId(courseId, pageable).map(chapterMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ChapterResponse getChapterById(Long courseId, Long chapterId) {
        Chapter chapter = chapterRepository.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found"));

        return chapterMapper.toResponse(chapter);
    }

    @Transactional
    public ChapterResponse createChapter(
            Long courseId,
            ChapterCreateRequest request
    ) {
        log.info("Creating new chapter");
        log.debug("Chapter request: courseId={}, data={}", courseId, request);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        Chapter chapter = chapterMapper.toEntity(request);
        chapter.setCourse(course);

        ChapterResponse response = chapterMapper.toResponse(chapterRepository.save(chapter));
        log.debug("Persisted chapter data: {}, courseId={}", response, courseId);
        return response;
    }

    @Transactional
    public ChapterResponse updateChapter(Long courseId,Long id, ChapterUpdateRequest request) {
        log.info("Updating chapter");
        log.debug("Chapter id={}, courseId={}", id, courseId);

        Chapter chapter = chapterRepository.findByIdAndCourseId(id, courseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Chapter not found")
                );

        chapterMapper.updateEntity(request, chapter);

        chapterRepository.flush();
        return chapterMapper.toResponse(chapter);
    }

    @Transactional
    public void deleteChapterByCourseId(Long id, Long courseId) {
        log.info("Deleting chapter");
        log.debug("Chapter id={}, courseId={}", id, courseId);

        Chapter entity = chapterRepository.findByIdAndCourseId(id, courseId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found"));

        try {
            chapterRepository.delete(entity);
            // Execute DELETE here so constraint violations can be translated to HTTP 409.
            chapterRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete chapter: it contains lessons",
                    exception
            );
        }
    }
}
