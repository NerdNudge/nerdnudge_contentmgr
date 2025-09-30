package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

@Data
public class UserTopicsStatsEntityResponse {
    private String lastTaken;
    private int level;
    private double progress;
}
