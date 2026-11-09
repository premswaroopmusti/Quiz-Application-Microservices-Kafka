package com.example.quiz_history_service.controller;


import com.example.quiz_history_service.entity.QuizHistory;
import com.example.quiz_history_service.repository.QuizHistoryRepository;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/history")
public class QuizHistoryController {

    private final QuizHistoryRepository quizHistoryRepository;

    public QuizHistoryController(QuizHistoryRepository quizHistoryRepository) {
        this.quizHistoryRepository = quizHistoryRepository;
    }

    @GetMapping
    public List<QuizHistory> getAllHistory() {
        return quizHistoryRepository.findAll();
    }

    @GetMapping("/quiz/{quizId}")
    public List<QuizHistory> getHistoryByQuizId(@PathVariable Integer quizId) {
        return quizHistoryRepository.findByQuizId(quizId);
    }
}