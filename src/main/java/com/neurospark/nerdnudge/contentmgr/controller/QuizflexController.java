package com.neurospark.nerdnudge.contentmgr.controller;

import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;
import com.neurospark.nerdnudge.contentmgr.service.QuizflexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/nerdnudge/quizflexes")
public class QuizflexController {
    @Autowired
    QuizflexService quizflexService;

    @GetMapping("/getall")
    public List<QuizflexEntity> getAllQuizFlexes(String topic, String subtopic, int limit) {
        return quizflexService.getAllQuizFlexes(topic, subtopic, limit);
    }

    @GetMapping("/get/{id}")
    public QuizflexEntity getQuizFlex(@PathVariable(value = "id") String quizflexId) {
        return quizflexService.getQuizFlex(quizflexId);
    }
}
