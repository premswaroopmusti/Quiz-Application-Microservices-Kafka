package com.example.quiz_history_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.quiz_history_service.entity.QuizHistory;

import java.util.List;

public interface QuizHistoryRepository extends JpaRepository<QuizHistory, Long> {

    boolean existsByEventId(String eventId);

    List<QuizHistory> findByQuizId(Integer quizId);

}