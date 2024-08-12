package com.neurospark.nerdnudge.contentmgr.controller;

import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.contentmgr.service.QuizflexService;
import com.neurospark.nerdnudge.contentmgr.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/nerdnudge/quizflexes")
public class QuizflexController {
    @Autowired
    QuizflexService quizflexService;

    @GetMapping("/getQuizFlexes")
    public ApiResponse<List<QuizflexEntity>> getQuizFlexes(
            @RequestParam String topic,
            @RequestParam String subtopic,
            @RequestParam int limit) throws Exception {
        long startTime = System.currentTimeMillis();
        List<QuizflexEntity> quizflexes = quizflexService.getQuizFlexes(topic, subtopic, limit);
        long endTime = System.currentTimeMillis();
        return new ApiResponse<>(Constants.SUCCESS, "Quizflexes fetched successfully", quizflexes, (endTime - startTime));
    }

    @GetMapping("/get/{id}")
    public ApiResponse<QuizflexEntity> getQuizFlexById(@PathVariable(value = "id") String quizflexId) throws Exception {
        long startTime = System.currentTimeMillis();
        QuizflexEntity quizflexEntityResponse = quizflexService.getQuizFlexById(quizflexId);
        long endTime = System.currentTimeMillis();
        return new ApiResponse<>(Constants.SUCCESS, "Quizflex fetched successfully", quizflexEntityResponse, (endTime - startTime));
    }
}
