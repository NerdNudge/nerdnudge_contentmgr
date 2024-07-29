package com.neurospark.nerdnudge.contentmgr.controller;

import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import com.neurospark.nerdnudge.contentmgr.service.TopicsService;
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
    public List<TopicsEntity> getTopics() {
        return topicsService.getTopics();
    }

    @GetMapping("/getsubtopics")
    public List<SubtopicsEntity> getSubtopics(String topic) {
        return topicsService.getSubtopics(topic);
    }
}
