package com.neurospark.nerdnudge.contentmgr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neurospark.nerdnudge.contentmgr.dto.*;
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
    private Map<String, Integer> userLevelConfig;
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

    private Map<String, UserTopicsStatsEntityResponse> getUserStats(String userId) {
        log.info("Fetching user topic stats for: {}", userId);
        RestTemplate restTemplate = new RestTemplate();
        Map<String, UserTopicsStatsEntityResponse> userTopicsStatsResponse = new HashMap<>();
        String userTopicStatsPath = "/getUserTopicStats/" + userId;
        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(userInsightsEndpoint + userTopicStatsPath, ApiResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                log.info("Response from insights: {}", apiResponse.getData());
                Map<String, LinkedHashMap<String, Object>> rawMap = (Map<String, LinkedHashMap<String, Object>>) apiResponse.getData();

                ObjectMapper mapper = new ObjectMapper();
                for (Map.Entry<String, LinkedHashMap<String, Object>> entry : rawMap.entrySet()) {
                    UserTopicsStatsEntity entity = mapper.convertValue(entry.getValue(), UserTopicsStatsEntity.class);
                    UserTopicsStatsEntityResponse responseDto = mapUserTopicsStatsEntityToResponse(entity, entry.getKey());
                    userTopicsStatsResponse.put(entry.getKey(), responseDto);
                }
                log.info("Response mapped: {}", userTopicsStatsResponse);
            } else {
                log.warn("Failed to retrieve user topic stats data.");
            }
        } catch (Exception e) {
            log.error("Exception while calling user topic stats API");
            e.printStackTrace();
        }

        return userTopicsStatsResponse.isEmpty() ? Collections.emptyMap() : userTopicsStatsResponse;
    }

    private UserTopicsStatsEntityResponse mapUserTopicsStatsEntityToResponse(UserTopicsStatsEntity userTopicsStatsEntity, String topic) {
        UserTopicsStatsEntityResponse userTopicsStatsEntityResponse = new UserTopicsStatsEntityResponse();
        userTopicsStatsEntityResponse.setLastTaken(userTopicsStatsEntity.getLastTaken());
        userTopicsStatsEntityResponse.setLevel(userTopicsStatsEntity.getLevel());
        userTopicsStatsEntityResponse.setProgress(getTopicProgress(userTopicsStatsEntity, topic));
        return userTopicsStatsEntityResponse;
    }

    private double getTopicProgress(UserTopicsStatsEntity userTopicsStatsEntity, String topic) {
        log.info("Getting Progress for topic: {}", topic);
        Map<String, String> subtopics = getSubtopicDataFromCache(topicCodeToTopicNameMapping.get(topic).getAsString());
        log.info("subtopics: {}", subtopics);
        double progress = (userTopicsStatsEntity.getLevel() * 100 ) / subtopics.size();
        log.info("Progress for topic: {}", progress);
        return progress;
    }

    private void updateUserLevelsConfigCache() {
        userLevelConfig = new HashMap<>();
        JsonObject nerdConfigObject = configPersist.get("nerd_config");
        JsonObject userLevelsObject = nerdConfigObject.get("userLevels").getAsJsonObject();
        Iterator<Map.Entry<String, JsonElement>> userLevelsIterator = userLevelsObject.entrySet().iterator();
        while(userLevelsIterator.hasNext()) {
            Map.Entry<String, JsonElement> thisLevelEntry = userLevelsIterator.next();
            String thisLevel = thisLevelEntry.getKey();
            int thisLevelTarget = thisLevelEntry.getValue().getAsJsonObject().get("target").getAsInt();
            userLevelConfig.put(thisLevel, thisLevelTarget);
        }
        log.info("user levels config cache: {}", userLevelConfig);
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
            updateTopicsCache();
        }

        return topicsEntities;
    }

    private Map<String, Integer> getUserLevelsConfigFromCache() {
        if(userLevelConfig == null || ! isWithinRetentionTime())
            updateUserLevelsConfigCache();

        return userLevelConfig;
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
        subtopicsEntity.setUserLevelTargetsConfig(getUserLevelsConfigFromCache());
        subtopicsEntity.setUserSubtopicLevels(getUserSubtopicLevels(topic, userId));
        return subtopicsEntity;
    }

    public Map<String, String> getUserSubtopicLevels(String topic, String userId) {
        log.info("Fetching user subtopic levels for: {}, topic: {}", userId, topic);
        RestTemplate restTemplate = new RestTemplate();
        String userSubtopicLevelsPath = "/getUserSubtopicLevels/" + topic + "/" + userId;
        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(userInsightsEndpoint + userSubtopicLevelsPath, ApiResponse.class);
            log.info("Response from insights: {}", response);
            log.info("{}", response.getStatusCode());
            log.info("{}", response.getBody());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                log.info("Response from insights for user subtopic levels: {}", apiResponse.getData());
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> userTopicsStatsResponse = mapper.convertValue(apiResponse.getData(), new TypeReference<Map<String, String>>() {});
                log.info("Response from user subtopic levels: {}", userTopicsStatsResponse);
                return userTopicsStatsResponse;
            } else {
                log.warn("Failed to retrieve user topic stats data.");
            }
        } catch (Exception e) {
            log.error("Exception while calling user topic stats API");
            e.printStackTrace();
        }

        return new HashMap<>();
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
        topicsWithUserTopicStatsEntity.setUserStats(getUserStats(userId));

        return topicsWithUserTopicStatsEntity;
    }
}
