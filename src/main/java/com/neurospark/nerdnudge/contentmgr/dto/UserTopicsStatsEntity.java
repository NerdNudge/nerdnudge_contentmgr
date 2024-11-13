package com.neurospark.nerdnudge.contentmgr.dto;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class UserTopicsStatsEntity {
    private double personalScoreIndicator;
    private String lastTaken;
    private JsonObject rwc;
}
