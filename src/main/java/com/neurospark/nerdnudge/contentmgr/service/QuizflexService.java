package com.neurospark.nerdnudge.contentmgr.service;

import com.google.gson.JsonArray;
import com.neurospark.nerdnudge.contentmgr.dto.FavoriteQuizflexEntity;
import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;

import java.util.List;

public interface QuizflexService {
    List<QuizflexEntity> getQuizFlexes(String topic, String subtopic, int limit) throws Exception;
    List<QuizflexEntity> getRealworldChallenge(String topic, String subtopic, int limit) throws Exception;

    QuizflexEntity getQuizflexById(String id) throws Exception;

    List<QuizflexEntity> getFavoriteQuizflexesByIds(JsonArray ids) throws Exception;
}
