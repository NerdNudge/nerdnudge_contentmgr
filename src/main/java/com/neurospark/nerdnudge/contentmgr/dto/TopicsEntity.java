package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

@Data
public class TopicsEntity {
    String topicName;
    int numPeopleTaken;
    String lastTakenByUser;
}
