package com.quizapp.quizapp.classassignment;

import com.quizapp.quizapp.subject.Subject;
import com.quizapp.quizapp.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "class_assignments", indexes = {
        @Index(name = "idx_class_subject", columnList = "class_name, subject_id", unique = false)
})
public class ClassAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "class_name", nullable = false, length = 50)
    private String className;

    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
}


