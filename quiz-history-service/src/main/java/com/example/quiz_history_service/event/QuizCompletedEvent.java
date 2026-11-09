package com.example.quiz_history_service.event;

import java.time.LocalDateTime;

public class QuizCompletedEvent {

    private String eventId;
    private Integer quizId;
    private Integer score;
    private Integer totalQuestions;
    private LocalDateTime completedAt;

    public QuizCompletedEvent() {
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

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}