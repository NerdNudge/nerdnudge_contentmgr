package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

@Data
public class TopicsEntity {
    String topicName;
    int numPeopleTaken = 0;
    double userScoreIndicator = 0.0;
    String lastTakenByUser = "Never";
}
