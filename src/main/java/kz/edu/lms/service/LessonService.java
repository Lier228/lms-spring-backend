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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Chapter not found"
            );
        }

        return lessonRepository
                .findAllByChapterId(chapterId,pageable)
                .map(lessonMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(Long chapterId, Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndChapterId(lessonId, chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lesson not found"
                ));

        return lessonMapper.toResponse(lesson);
    }

    @Transactional
    public LessonResponse createLesson(
            Long chapterId,
            LessonCreateRequest request
    ) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Chapter not found"
                ));

        Lesson lesson = lessonMapper.toEntity(request);
        lesson.setChapter(chapter);

        return lessonMapper.toResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateLesson(Long chapterId,Long id, LessonUpdateRequest request) {
        Lesson lesson = lessonRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Lesson not found"
                        )
                );

        lessonMapper.updateEntity(request, lesson);

        lessonRepository.flush();
        return lessonMapper.toResponse(lesson);
    }

    @Transactional
    public void deleteLessonByChapterId(Long id, Long chapterId) {
        Lesson entity = lessonRepository.findByIdAndChapterId(id, chapterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lesson not found"
                ));


        lessonRepository.delete(entity);
        lessonRepository.flush();
    }
}
