package com.neurospark.nerdnudge.contentmgr.service;

import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicsServiceImpl implements TopicsService{
    @Override
    public List<TopicsEntity> getTopics() {
        return null;
    }

    @Override
    public List<SubtopicsEntity> getSubtopics(String topic) {
        return null;
    }
}
