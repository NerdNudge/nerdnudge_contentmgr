package com.neurospark.nerdnudge.contentmgr.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsWithUserTopicStatsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.UserTopicsStatsEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.couchbase.service.NerdPersistClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class TopicsServiceImpl implements TopicsService{

    private Map<String, TopicsEntity> topicsEntities = null;
    private Map<String, Map<String, String>> subtopicCache = null;
    private Map<String, String> topicsConfigCache;
    private long lastFetchTime;
    private int retentionInMillis = 60 * 60 * 1000;
    private final NerdPersistClient configPersist;
    private JsonObject topicCodeToTopicNameMapping;
    private JsonObject topicNameToTopicCodeMapping;
    private NerdPersistClient shotsStatsPersist;

    @Value("${api.endpoint.user.insights}")
    private String userInsightsEndpoint;

    public TopicsServiceImpl(@Qualifier("configPersist") NerdPersistClient configPersist,
                             @Qualifier("shotsStatsPersist") NerdPersistClient shotsStatsPersist) {
        this.configPersist = configPersist;
        this.shotsStatsPersist = shotsStatsPersist;
        subtopicCache = new HashMap<>();
        updateTopicsCache();
    }

    private Map<String, UserTopicsStatsEntity> getUserStats(String userId) {
        log.info("Fetching user topic stats for: {}", userId);
        RestTemplate restTemplate = new RestTemplate();
        Map<String, UserTopicsStatsEntity> userTopicsStatsEntities = null;
        String userTopicStatsPath = "/getUserTopicStats/" + userId;
        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(userInsightsEndpoint + userTopicStatsPath, ApiResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                userTopicsStatsEntities = (Map<String, UserTopicsStatsEntity>) apiResponse.getData();
            } else {
                log.warn("Failed to retrieve user topic stats data.");
            }
        } catch (Exception e) {
            log.error("Exception while calling user topic stats API");
            e.printStackTrace();
        }

        return userTopicsStatsEntities;
    }

    private void updateTopicsConfigCache() {
        topicsConfigCache = new HashMap<>();
        JsonObject collectionMapping = configPersist.get("nerd_config");
        topicsConfigCache.put("rwcDailyQuizLimit", collectionMapping.get("rwcDailyQuizLimit").getAsString());
        topicsConfigCache.put("rwcDailyQuizTime", collectionMapping.get("rwcDailyQuizTime").getAsString());
        log.info("topics config cache: {}", topicsConfigCache);
    }

    private void updateTopicsCache() {
        topicsEntities = new HashMap<>();
        topicCodeToTopicNameMapping = configPersist.get("collection_topic_mapping");
        topicNameToTopicCodeMapping = new JsonObject();
        Iterator<Map.Entry<String, JsonElement>> topicsIterator = topicCodeToTopicNameMapping.entrySet().iterator();
        while(topicsIterator.hasNext()) {
            Map.Entry<String, JsonElement> thisEntry = topicsIterator.next();

            TopicsEntity topicsEntity = new TopicsEntity();
            topicsEntity.setTopicName(thisEntry.getValue().getAsString());
            topicsEntity.setNumPeopleTaken((int) shotsStatsPersist.getCounter(thisEntry.getKey() + "_user_count"));

            topicsEntities.put(thisEntry.getKey(), topicsEntity);
            topicNameToTopicCodeMapping.addProperty(thisEntry.getValue().getAsString(), thisEntry.getKey());
        }
        lastFetchTime = System.currentTimeMillis();

        log.info("Topic Code to Topic Name Mapping: {}", topicCodeToTopicNameMapping);
        log.info("Topic Name to Topic Code Mapping: {}", topicNameToTopicCodeMapping);
    }


    private Map<String, TopicsEntity> getTopicsFromCache() {
        if(topicsEntities == null || ! isWithinRetentionTime()) {
            updateTopicsConfigCache();
            updateTopicsCache();
        }

        return topicsEntities;
    }

    private Map<String, String> getTopicsConfigFromCache() {
        if(topicsConfigCache == null || ! isWithinRetentionTime())
            updateTopicsConfigCache();

        return topicsConfigCache;
    }

    private boolean isWithinRetentionTime() {
        long currentTimeMillis = System.currentTimeMillis();
        long timeElapsedSinceLastFetch = currentTimeMillis - lastFetchTime;
        return timeElapsedSinceLastFetch <= retentionInMillis;
    }

    @Override
    public SubtopicsEntity getSubtopics(String topic, String userId) {
        SubtopicsEntity subtopicsEntity = new SubtopicsEntity();
        subtopicsEntity.setSubtopicData(getSubtopicDataFromCache(topic));
        subtopicsEntity.setUserTopicScore(getUserScore(userId, topic));
        return subtopicsEntity;
    }

    private void updateSubtopicsCache(String topic) {
        if(subtopicCache == null) {
            subtopicCache = new HashMap<>();
        }

        JsonObject subtopicsObject = configPersist.get(topicNameToTopicCodeMapping.get(topic).getAsString() + "_subtopics");
        Iterator<Map.Entry<String, JsonElement>> subtopicsIterator = subtopicsObject.entrySet().iterator();
        Map<String, String> thisTopicSubtopics = new LinkedHashMap<>();
        while(subtopicsIterator.hasNext()) {
            Map.Entry<String, JsonElement> thisEntry = subtopicsIterator.next();
            String value = thisEntry.getValue().getAsString();
            if (value != null) {
                thisTopicSubtopics.put(thisEntry.getKey(), value);
            }
        }
        subtopicCache.put(topic, thisTopicSubtopics);
        log.info("sub-topics for topic: {}: {}", topic, thisTopicSubtopics);
    }

    private Map<String, String> getSubtopicDataFromCache(String topic) {
        if(subtopicCache == null || ! subtopicCache.containsKey(topic)) {
            updateSubtopicsCache(topic);
        }
        return subtopicCache.get(topic);
    }

    @Override
    public TopicsWithUserTopicStatsEntity getTopics(String userId) {
        TopicsWithUserTopicStatsEntity topicsWithUserTopicStatsEntity = new TopicsWithUserTopicStatsEntity();
        topicsWithUserTopicStatsEntity.setTopics(getTopicsFromCache());
        topicsWithUserTopicStatsEntity.setConfig(getTopicsConfigFromCache());
        topicsWithUserTopicStatsEntity.setUserStats(getUserStats(userId));

        return topicsWithUserTopicStatsEntity;
    }

    private double getUserScore(String userId, String topic) {
        RestTemplate restTemplate = new RestTemplate();
        Double userTopicScore = 0.0;
        String topicCode = topicNameToTopicCodeMapping.get(topic).getAsString();
        String userTopicScorePath = "/getUserTopicScore/" + userId + "/" + topicCode;
        log.info("Fetching user topic score for: {}, topic: {}, topicCode: {}", userId, topic, topicCode);
        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(userInsightsEndpoint + userTopicScorePath, ApiResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                userTopicScore = (Double) apiResponse.getData();
            } else {
                log.warn("Failed to retrieve user topic score.");
            }
        } catch (Exception e) {
            log.error("Exception while calling user topic score API");
            e.printStackTrace();
        }

        return userTopicScore;
    }
}
