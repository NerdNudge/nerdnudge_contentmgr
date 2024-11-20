package com.neurospark.nerdnudge.contentmgr.controller;

import com.google.gson.JsonParser;
import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.contentmgr.service.QuizflexService;
import com.neurospark.nerdnudge.contentmgr.utils.Constants;
import com.neurospark.nerdnudge.metrics.logging.NerdLogger;
import com.neurospark.nerdnudge.metrics.metrics.Metric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Slf4j
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
        new Metric.MetricBuilder().setName("quizFetch").setUnit(Metric.Unit.MILLISECONDS).setValue((endTime - startTime)).build();

        return new ApiResponse<>(Constants.SUCCESS, "Quizflexes fetched successfully", quizflexes, (endTime - startTime), HttpStatus.OK.value());
    }

    @GetMapping("/getRealworldChallenges")
    public ApiResponse<List<QuizflexEntity>> getRealworldChallenges(
            @RequestParam String topic,
            @RequestParam String subtopic,
            @RequestParam int limit) throws Exception {
        long startTime = System.currentTimeMillis();
        List<QuizflexEntity> realworldChallenges = quizflexService.getRealworldChallenge(topic, subtopic, limit);
        long endTime = System.currentTimeMillis();
        new Metric.MetricBuilder().setName("rwcFetch").setUnit(Metric.Unit.MILLISECONDS).setValue((endTime - startTime)).build();
        return new ApiResponse<>(Constants.SUCCESS, "RWC fetched successfully", realworldChallenges, (endTime - startTime), HttpStatus.OK.value());
    }

    @GetMapping("/get/{id}")
    public ApiResponse<QuizflexEntity> getQuizflexById(@PathVariable(value = "id") String quizflexId) throws Exception {
        long startTime = System.currentTimeMillis();
        QuizflexEntity quizflexEntityResponse = quizflexService.getQuizflexById(quizflexId);
        long endTime = System.currentTimeMillis();
        new Metric.MetricBuilder().setName("getQFById").setUnit(Metric.Unit.MILLISECONDS).setValue((endTime - startTime)).build();
        return new ApiResponse<>(Constants.SUCCESS, "Quizflex fetched successfully", quizflexEntityResponse, (endTime - startTime), HttpStatus.OK.value());
    }

    @PostMapping("/getFavoriteQuizflexesByIds")
    public ApiResponse<List<QuizflexEntity>> getFavoriteQuizflexesByIds(@RequestBody String idsJsonArray) throws Exception {
        long startTime = System.currentTimeMillis();
        com.google.gson.JsonArray idsArray = new JsonParser().parse(idsJsonArray).getAsJsonArray();
        List<QuizflexEntity> result = quizflexService.getFavoriteQuizflexesByIds(idsArray);
        long endTime = System.currentTimeMillis();
        new Metric.MetricBuilder().setName("favQFFetch").setUnit(Metric.Unit.MILLISECONDS).setValue((endTime - startTime)).build();
        return new ApiResponse<>(Constants.SUCCESS, "Favorite Quizflexes fetched successfully", result, (endTime - startTime), HttpStatus.OK.value());
    }

    @GetMapping("/health")
    public ApiResponse<String> healthCheck() {
        return new ApiResponse<>("SUCCESS", "Health Check Pass", "SUCCESS", 0, HttpStatus.OK.value());
    }
}
