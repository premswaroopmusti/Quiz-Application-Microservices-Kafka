package com.example.quiz_service.kafka;

import com.example.quiz_service.event.QuizCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class QuizProducer {

    private static final String TOPIC = "quiz-completed";

    private final KafkaTemplate<String, QuizCompletedEvent> kafkaTemplate;

    public QuizProducer(KafkaTemplate<String, QuizCompletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendQuizCompletedEvent(QuizCompletedEvent event) {

        kafkaTemplate.send(
                TOPIC,
                event.getEventId(),
                event
        );

        System.out.println("Quiz completed event sent to Kafka");
    }
}