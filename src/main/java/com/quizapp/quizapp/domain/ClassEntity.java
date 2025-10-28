package com.quizapp.quizapp.domain;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

import com.quizapp.quizapp.user.User; // <-- THIS import fixes the errors

@Entity
@Table(name = "classes")
public class ClassEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    // One teacher per class
    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    // Many students per class
    @ManyToMany
    @JoinTable(
        name = "class_students",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<User> students = new HashSet<>();

    // Getters and Setters
    public Integer getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Set<User> getStudents() { return students; }
    public void setStudents(Set<User> students) { this.students = students; }
}
