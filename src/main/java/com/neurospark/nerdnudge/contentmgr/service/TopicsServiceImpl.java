package com.neurospark.nerdnudge.contentmgr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neurospark.nerdnudge.contentmgr.dto.SubtopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.TopicsEntity;
import com.neurospark.nerdnudge.contentmgr.dto.UserTopicsStatsEntity;
import com.neurospark.nerdnudge.contentmgr.response.ApiResponse;
import com.neurospark.nerdnudge.couchbase.service.NerdPersistClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class TopicsServiceImpl implements TopicsService{

    private List<TopicsEntity> topicsEntities = null;
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
        updateTopicsCache();
    }

    @Override
    public List<TopicsEntity> getTopics(String userId) {
        List<TopicsEntity> topicsEntity = getTopicsFromCache();
        updateUserStats(topicsEntity, userId);
        return topicsEntity;
    }

    private void updateUserStats(List<TopicsEntity> topicsEntity, String userId) {
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("Now trying to call api and fetch user topic stats.");
        Map<String, UserTopicsStatsEntity> userTopicsStatsEntities = null;
        String userTopicStatsPath = "/getUserTopicStats/" + userId;
        try {
            ResponseEntity<ApiResponse> response = restTemplate.getForEntity(userInsightsEndpoint + userTopicStatsPath, ApiResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ApiResponse apiResponse = response.getBody();
                userTopicsStatsEntities = (Map<String, UserTopicsStatsEntity>) apiResponse.getData();
            } else {
                System.out.println("Failed to retrieve data.");
            }
        } catch (Exception e) {
            System.out.println("Exception while calling API");
            e.printStackTrace();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            for (int i = 0; i < topicsEntity.size(); i++) {
                TopicsEntity thisTopicEntity = topicsEntity.get(i);
                UserTopicsStatsEntity thisUserTopicStats = null;
                if (userTopicsStatsEntities != null && userTopicsStatsEntities.containsKey(thisTopicEntity.getTopicName())) {
                    Object statsObject = userTopicsStatsEntities.get(thisTopicEntity.getTopicName());
                    if (statsObject instanceof LinkedHashMap) {
                        thisUserTopicStats = objectMapper.convertValue(statsObject, UserTopicsStatsEntity.class);
                    } else if (statsObject instanceof UserTopicsStatsEntity) {
                        thisUserTopicStats = (UserTopicsStatsEntity) statsObject;
                    }

                    thisTopicEntity.setLastTakenByUser(thisUserTopicStats.getLastTaken());
                    thisTopicEntity.setUserScoreIndicator(thisUserTopicStats.getPersonalScoreIndicator());
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateTopicsCache() {
        topicsEntities = new ArrayList<>();
        topicCodeToTopicNameMapping = configPersist.get("collection_topic_mapping");
        topicNameToTopicCodeMapping = new JsonObject();
        Iterator<Map.Entry<String, JsonElement>> topicsIterator = topicCodeToTopicNameMapping.entrySet().iterator();
        while(topicsIterator.hasNext()) {
            Map.Entry<String, JsonElement> thisEntry = topicsIterator.next();

            TopicsEntity topicsEntity = new TopicsEntity();
            topicsEntity.setTopicName(thisEntry.getValue().getAsString());
            topicsEntity.setNumPeopleTaken((int) shotsStatsPersist.getCounter(thisEntry.getKey() + "_user_count"));

            topicsEntities.add(topicsEntity);
            topicNameToTopicCodeMapping.addProperty(thisEntry.getValue().getAsString(), thisEntry.getKey());
        }

        System.out.println(topicCodeToTopicNameMapping);
        System.out.println(topicNameToTopicCodeMapping);
    }


    private List<TopicsEntity> getTopicsFromCache() {
        if(topicsEntities == null || ! isWithinRetentionTime())
            updateTopicsCache();

        return topicsEntities;
    }

    private boolean isWithinRetentionTime() {
        long currentTimeMillis = System.currentTimeMillis();
        long timeElapsedSinceLastFetch = currentTimeMillis - lastFetchTime;
        return timeElapsedSinceLastFetch <= retentionInMillis;
    }

    @Override
    public List<SubtopicsEntity> getSubtopics(String topic) {
        System.out.println("Fetching subtopics for: " + topicNameToTopicCodeMapping.get(topic).getAsString());
        List<SubtopicsEntity> subtopicsEntities = new ArrayList<>();
        JsonObject subtopicsObject = configPersist.get(topicNameToTopicCodeMapping.get(topic).getAsString() + "_subtopics");
        Iterator<Map.Entry<String, JsonElement>> subtopicsIterator = subtopicsObject.entrySet().iterator();
        while(subtopicsIterator.hasNext()) {
            Map.Entry<String, JsonElement> thisEntry = subtopicsIterator.next();
            String value = thisEntry.getValue().getAsString();
            if (value != null) {
                SubtopicsEntity currentSubtopic = new SubtopicsEntity();
                currentSubtopic.setSubtopicName(thisEntry.getKey());
                currentSubtopic.setDescription(value);

                subtopicsEntities.add(currentSubtopic);
            }
        }
        return subtopicsEntities;
    }
}
