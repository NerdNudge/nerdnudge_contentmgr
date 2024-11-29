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
    private Map<String, List<SubtopicsEntity>> subtopicEntities = null;
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
        subtopicEntities = new HashMap<>();
        updateTopicsCache();
    }

    @Override
    public TopicsWithUserTopicStatsEntity getTopics(String userId) {
        TopicsWithUserTopicStatsEntity topicsWithUserTopicStatsEntity = new TopicsWithUserTopicStatsEntity();
        topicsWithUserTopicStatsEntity.setTopics(getTopicsFromCache());
        topicsWithUserTopicStatsEntity.setConfig(getTopicsConfigFromCache());
        topicsWithUserTopicStatsEntity.setUserStats(getUserStats(userId));

        return topicsWithUserTopicStatsEntity;
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
    public List<SubtopicsEntity> getSubtopics(String topic) {
        if(subtopicEntities.containsKey(topic)) {
            log.info("Returning subtopics from cache for topic: {}", topic);
            return subtopicEntities.get(topic);
        }

        log.info("Fetching subtopics for topic: {}", topicNameToTopicCodeMapping.get(topic).getAsString());
        List<SubtopicsEntity> subtopicEntitiesList = new ArrayList<>();
        JsonObject subtopicsObject = configPersist.get(topicNameToTopicCodeMapping.get(topic).getAsString() + "_subtopics");
        Iterator<Map.Entry<String, JsonElement>> subtopicsIterator = subtopicsObject.entrySet().iterator();
        while(subtopicsIterator.hasNext()) {
            Map.Entry<String, JsonElement> thisEntry = subtopicsIterator.next();
            String value = thisEntry.getValue().getAsString();
            if (value != null) {
                SubtopicsEntity currentSubtopic = new SubtopicsEntity();
                currentSubtopic.setSubtopicName(thisEntry.getKey());
                currentSubtopic.setDescription(value);

                subtopicEntitiesList.add(currentSubtopic);
            }
        }
        subtopicEntities.put(topic, subtopicEntitiesList);
        return subtopicEntitiesList;
    }
}
