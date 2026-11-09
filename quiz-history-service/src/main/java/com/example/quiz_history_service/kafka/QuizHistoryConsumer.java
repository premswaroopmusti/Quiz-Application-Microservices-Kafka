package com.example.quiz_history_service.kafka;

import com.example.quiz_history_service.entity.QuizHistory;
import com.example.quiz_history_service.event.QuizCompletedEvent;
import com.example.quiz_history_service.repository.QuizHistoryRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class QuizHistoryConsumer {

    private final QuizHistoryRepository quizHistoryRepository;

    public QuizHistoryConsumer(QuizHistoryRepository quizHistoryRepository) {
        this.quizHistoryRepository = quizHistoryRepository;
    }

    @KafkaListener(
            topics = "quiz-completed",
            groupId = "quiz-history-group"
    )
    public void consumeQuizCompletedEvent(QuizCompletedEvent event) {

        System.out.println("Received quiz completed event: " + event.getEventId());

        if (quizHistoryRepository.existsByEventId(event.getEventId())) {
            System.out.println("Event already processed. Skipping duplicate.");
            return;
        }

        QuizHistory history = new QuizHistory();

        history.setEventId(event.getEventId());
        history.setQuizId(event.getQuizId());
        history.setScore(event.getScore());
        history.setTotalQuestions(event.getTotalQuestions());
        history.setCompletedAt(event.getCompletedAt());

        quizHistoryRepository.save(history);

        System.out.println("Quiz history saved successfully!");
    }
}