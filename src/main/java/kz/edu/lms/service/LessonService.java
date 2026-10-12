package kz.edu.lms.service;

import kz.edu.lms.dto.lesson.LessonCreateRequest;
import kz.edu.lms.dto.lesson.LessonResponse;
import kz.edu.lms.dto.lesson.LessonUpdateRequest;
import kz.edu.lms.entity.Chapter;
import kz.edu.lms.entity.Lesson;
import kz.edu.lms.mapper.LessonMapper;
import kz.edu.lms.repository.ChapterRepository;
import kz.edu.lms.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonMapper lessonMapper;
    private final LessonRepository lessonRepository;
    private final ChapterRepository chapterRepository;

    @Transactional(readOnly = true)
    public Page<LessonResponse> getAll(Pageable pageable) {
        return lessonRepository.findAll(pageable).map(lessonMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<LessonResponse> getAllByChapterId(Long chapterId, Pageable pageable) {
        if (!chapterRepository.existsById(chapterId)) {
            throw new IllegalArgumentException("Chapter not found");
        }

        return lessonRepository
                .findAllByChapterId(chapterId,pageable)
                .map(lessonMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(Long chapterId, Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndChapterId(lessonId, chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));

        return lessonMapper.toResponse(lesson);
    }

    @Transactional
    public LessonResponse createLesson(
            Long chapterId,
            LessonCreateRequest request
    ) {
        log.info("Creating new lesson");
        log.debug("Lesson request: chapterId={}, data={}", chapterId, request);

        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found"));

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setChapter(chapter);

        LessonResponse response = lessonMapper.toResponse(lessonRepository.save(lesson));
        log.debug("Persisted lesson data: {}, chapterId={}", response, chapterId);
        return response;
    }

    @Transactional
    public LessonResponse updateLesson(Long chapterId,Long id, LessonUpdateRequest request) {
        log.info("Updating lesson");
        log.debug("Lesson id={}, chapterId={}", id, chapterId);

        Lesson lesson = lessonRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lesson not found")
                );

        lessonMapper.updateEntity(request, lesson);

        lessonRepository.flush();
        return lessonMapper.toResponse(lesson);
    }

    @Transactional
    public void deleteLessonByChapterId(Long id, Long chapterId) {
        log.info("Deleting lesson");
        log.debug("Lesson id={}, chapterId={}", id, chapterId);

        Lesson entity = lessonRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));


        lessonRepository.delete(entity);
        lessonRepository.flush();
    }
}
