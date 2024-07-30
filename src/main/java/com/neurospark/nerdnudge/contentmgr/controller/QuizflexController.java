package com.neurospark.nerdnudge.contentmgr.controller;

import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;
import com.neurospark.nerdnudge.contentmgr.service.QuizflexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/nerdnudge/quizflexes")
public class QuizflexController {
    @Autowired
    QuizflexService quizflexService;

    @GetMapping("/getQuizFlexes")
    public List<QuizflexEntity> getQuizFlexes(
            @RequestParam String topic,
            @RequestParam String subtopic,
            @RequestParam int limit) {
        return quizflexService.getQuizFlexes(topic, subtopic, limit);
    }

    @GetMapping("/get/{id}")
    public QuizflexEntity getQuizFlexById(@PathVariable(value = "id") String quizflexId) {
        return quizflexService.getQuizFlexById(quizflexId);
    }
}
