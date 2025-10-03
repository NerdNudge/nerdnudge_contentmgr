package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SubtopicsEntity {
    Map<String, String> subtopicData;
    Map<String, Integer> userLevelTargetsConfig;
    Map<String, String> userSubtopicLevels;
}
