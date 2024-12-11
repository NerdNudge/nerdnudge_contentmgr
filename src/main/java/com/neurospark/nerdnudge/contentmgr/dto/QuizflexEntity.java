package com.neurospark.nerdnudge.contentmgr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizflexEntity {
    String id;
    String title;
    String question = "";
    Map<String, String> possible_answers = new HashMap<>();
    Map<String, Double> answer_percentages = new HashMap<>();
    String right_answer = "";
    String topic_name;
    String sub_topic;
    String description_and_explanation;
    String difficulty_level = "easy";
    int time_limit_secs = 60;
    String pro_tip;
    String fun_fact;

    long likes;
    long dislikes;
    long favorites;
    long shares;
}