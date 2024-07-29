package com.neurospark.nerdnudge.contentmgr.dto;

import lombok.Data;

import java.util.Map;

@Data
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

    int likes;
    int dislikes;
    int favorites;
    int shares;
}
