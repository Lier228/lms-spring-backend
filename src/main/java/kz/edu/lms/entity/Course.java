package kz.edu.lms.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "courses")
public class Course extends BaseEntity{
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "course_access", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "user_id", nullable = false)
    @Builder.Default
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private Set<String> readerIds = new HashSet<>();
}
