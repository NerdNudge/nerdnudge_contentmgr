package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

@Data
public class TopicsEntity {
    String topicName;
    String topicCode;
    int numPeopleTaken = 0;
    double userScoreIndicator = 0.0;
    String lastTakenByUser = "Never";
}
