package com.neurospark.nerdnudge.contentmgr.service;

import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;

import java.util.List;

public interface QuizflexService {
    List<QuizflexEntity> getQuizFlexes(String topic, String subtopic, int limit);

    QuizflexEntity getQuizFlexById(String id) throws Exception;
}
