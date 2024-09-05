package com.neurospark.nerdnudge.contentmgr.controller;

import com.couchbase.client.core.deps.com.google.api.Http;
import com.google.gson.JsonParser;
import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.contentmgr.service.QuizflexService;
import com.neurospark.nerdnudge.contentmgr.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        return new ApiResponse<>(Constants.SUCCESS, "Quizflexes fetched successfully", quizflexes, (endTime - startTime), HttpStatus.OK.value());
    }

    @GetMapping("/get/{id}")
    public ApiResponse<QuizflexEntity> getQuizflexById(@PathVariable(value = "id") String quizflexId) throws Exception {
        long startTime = System.currentTimeMillis();
        QuizflexEntity quizflexEntityResponse = quizflexService.getQuizflexById(quizflexId);
        long endTime = System.currentTimeMillis();
        return new ApiResponse<>(Constants.SUCCESS, "Quizflex fetched successfully", quizflexEntityResponse, (endTime - startTime), HttpStatus.OK.value());
    }

    @PostMapping("/getFavoriteQuizflexesByIds")
    public ApiResponse<List<QuizflexEntity>> getFavoriteQuizflexesByIds(@RequestBody String idsJsonArray) throws Exception {
        long startTime = System.currentTimeMillis();
        com.google.gson.JsonArray idsArray = new JsonParser().parse(idsJsonArray).getAsJsonArray();
        List<QuizflexEntity> result = quizflexService.getFavoriteQuizflexesByIds(idsArray);
        long endTime = System.currentTimeMillis();
        return new ApiResponse<>(Constants.SUCCESS, "Favorite Quizflexes fetched successfully", result, (endTime - startTime), HttpStatus.OK.value());
    }
}
