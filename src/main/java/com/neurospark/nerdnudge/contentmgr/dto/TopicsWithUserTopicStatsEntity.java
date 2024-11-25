package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TopicsWithUserTopicStatsEntity {
    Map<String, TopicsEntity> topics;
    Map<String, UserTopicsStatsEntity> userStats;
    Map<String, String> config;
}
