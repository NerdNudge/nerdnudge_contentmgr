package com.neurospark.nerdnudge.contentmgr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizflexEntity {
    String id;
    String title;
    String question;
    Map<String, String> possible_answers;
    Map<String, Double> answer_percentages;
    String right_answer;
    String topic_name;
    String sub_topic;
    String description_and_explanation;
    String difficulty_level;
    int time_limit_secs;
    String pro_tip;
    String fun_fact;

    long likes;
    long dislikes;
    long favorites;
    long shares;
}