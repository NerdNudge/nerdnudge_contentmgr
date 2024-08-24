package com.neurospark.nerdnudge.contentmgr.service;

import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;

import java.util.List;

public interface TopicsService {
    List<TopicsEntity> getTopics(String userId);

    List<SubtopicsEntity> getSubtopics(String topic);
}
