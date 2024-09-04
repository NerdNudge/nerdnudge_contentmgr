package com.neurospark.nerdnudge.contentmgr.service;

import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsWithUserTopicStatsEntity;

import java.util.List;

public interface TopicsService {
    TopicsWithUserTopicStatsEntity getTopics(String userId);

    List<SubtopicsEntity> getSubtopics(String topic);
}
