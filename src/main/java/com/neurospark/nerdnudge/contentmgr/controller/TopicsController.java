package com.neurospark.nerdnudge.contentmgr.controller;

import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.contentmgr.service.TopicsService;
import com.neurospark.nerdnudge.contentmgr.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/nerdnudge/topics")
public class TopicsController {
    @Autowired
    TopicsService topicsService;

    @GetMapping("/getall")
    public ApiResponse<List<TopicsEntity>> getTopics() {
        long startTime = System.currentTimeMillis();
        List<TopicsEntity> topicsEntityList = topicsService.getTopics();
        long endTime = System.currentTimeMillis();
        return new ApiResponse<>(Constants.SUCCESS, "Topics fetched successfully", topicsEntityList, (endTime - startTime));
    }

    @GetMapping("/getsubtopics")
    public ApiResponse<List<SubtopicsEntity>> getSubtopics(String topic) {
        long startTime = System.currentTimeMillis();
        List<SubtopicsEntity> subtopicsEntityList = topicsService.getSubtopics(topic);
        long endTime = System.currentTimeMillis();
        return new ApiResponse<>(Constants.SUCCESS, "Sub-topics fetched successfully", subtopicsEntityList, (endTime - startTime));
    }
}
