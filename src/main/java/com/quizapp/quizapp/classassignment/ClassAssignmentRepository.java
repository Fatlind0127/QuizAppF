package com.quizapp.quizapp.classassignment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassAssignmentRepository extends JpaRepository<ClassAssignment, Integer> {
    Optional<ClassAssignment> findByClassNameAndSubject_Id(String className, Integer subjectId);
    java.util.List<ClassAssignment> findAllBySubject_Id(Integer subjectId);
}


