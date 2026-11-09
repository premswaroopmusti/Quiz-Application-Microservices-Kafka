package com.example.quiz_service.service;

import com.example.quiz_service.Dao.QuizDao;
import com.example.quiz_service.feign.QuizInterface;
import com.example.quiz_service.model.QuestionWrapper;
import com.example.quiz_service.model.Quiz;
import com.example.quiz_service.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.example.quiz_service.kafka.QuizProducer;
import com.example.quiz_service.event.QuizCompletedEvent;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuizService {


            @Autowired
            QuizDao quizDao;

            @Autowired
            QuizProducer quizProducer;

            @Autowired
            QuizInterface quizInterface;

//            @Autowired
//            QuestionDao questionDao;

            public ResponseEntity<?> createQuiz(String category, int numQ, String title){

                     List<Integer> questions = quizInterface.getQuestionsForQuiz(category, numQ).getBody();

                     Quiz quiz = new Quiz();

                     quiz.setTitle(title);
                     quiz.setQuestionIds(questions);
                     quizDao.save(quiz);

                     return new ResponseEntity<>("Success", HttpStatus.CREATED);

            }

            public ResponseEntity<List<QuestionWrapper>> getQuizQuestions(Integer id){

                  Quiz quiz = quizDao.findById(id).get();

                  List<Integer> questionIds = quiz.getQuestionIds();

                  ResponseEntity<List<QuestionWrapper>> questions =  quizInterface.getQuestionsFromId(questionIds);


                return questions;

            }

    public ResponseEntity<Integer> calculateResult(Integer id, List<Response> responses) {

        // Step 1: Ask Question Service to calculate the score
        ResponseEntity<Integer> score = quizInterface.getScore(responses);

        // Step 2: Publish event only if score calculation succeeds
        if (score.getStatusCode().is2xxSuccessful() && score.getBody() != null) {

            // Step 3: Get quiz details from Quiz DB
            Quiz quiz = quizDao.findById(id).orElse(null);

            if (quiz != null) {

                // Step 4: Create event
                QuizCompletedEvent event = new QuizCompletedEvent(
                        id,
                        score.getBody(),
                        quiz.getQuestionIds().size()
                );

                // Step 5: Send event to Kafka
                quizProducer.sendQuizCompletedEvent(event);
            }
        }

        // Step 6: Return the original score to the client
        return score;
    }

    }