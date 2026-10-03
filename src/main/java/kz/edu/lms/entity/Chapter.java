package kz.edu.lms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Chapter extends BaseEntity{
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
