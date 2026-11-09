package com.example.quiz_service.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class QuizCompletedEvent {

    private String eventId;
    private Integer quizId;
    private Integer score;
    private Integer totalQuestions;
    private LocalDateTime completedAt;

    public QuizCompletedEvent() {
    }

    public QuizCompletedEvent(Integer quizId, Integer score, Integer totalQuestions) {
        this.eventId = UUID.randomUUID().toString();
        this.quizId = quizId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.completedAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}