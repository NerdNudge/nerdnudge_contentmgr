package com.neurospark.nerdnudge.contentmgr.controller;

import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsWithUserTopicStatsEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.contentmgr.service.TopicsService;
import com.neurospark.nerdnudge.contentmgr.utils.Constants;
import com.neurospark.nerdnudge.metrics.metrics.Metric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/nerdnudge/topics")
public class TopicsController {
    @Autowired
    TopicsService topicsService;

    @GetMapping("/getall/{id}")
    public ApiResponse<TopicsWithUserTopicStatsEntity> getTopics(@PathVariable(value = "id") String userId) {
        log.info("Getting topics data for user: {}", userId);
        long startTime = System.currentTimeMillis();
        TopicsWithUserTopicStatsEntity topicsWithUserStatsEntityList = topicsService.getTopics(userId);
        long endTime = System.currentTimeMillis();
        new Metric.MetricBuilder().setName("topicsFetch").setUnit(Metric.Unit.MILLISECONDS).setValue((endTime - startTime)).build();
        return new ApiResponse<>(Constants.SUCCESS, "Topics fetched successfully", topicsWithUserStatsEntityList, (endTime - startTime), HttpStatus.OK.value());
    }

    @GetMapping("/getsubtopics/{topic}")
    public ApiResponse<List<SubtopicsEntity>> getSubtopics(@PathVariable(value = "topic") String topic) {
        log.info("Fetching sub topics for topic: {}", topic);
        long startTime = System.currentTimeMillis();
        List<SubtopicsEntity> subtopicsEntityList = topicsService.getSubtopics(topic);
        long endTime = System.currentTimeMillis();
        new Metric.MetricBuilder().setName("subtopicsFetch").setUnit(Metric.Unit.MILLISECONDS).setValue((endTime - startTime)).build();
        return new ApiResponse<>(Constants.SUCCESS, "Sub-topics fetched successfully", subtopicsEntityList, (endTime - startTime), HttpStatus.OK.value());
    }
}
