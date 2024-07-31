package com.neurospark.nerdnudge.contentmgr.service;

import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.neurospark.nerdnudge.contentmgr.dto.QuizflexEntity;
import com.neurospark.nerdnudge.couchbase.service.NerdPersistClient;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizflexServiceImpl implements QuizflexService {

    private final Map<String, NerdPersistClient> nerdPersistClients;
    private Map<String, List<String>> topicwiseQuizIds;
    private Map<String, Map<String, List<String>>> subtopicwiseQuizIds;
    private Map<String, QuizflexEntity> contentMaster;

    private final JsonParser jsonParser = new JsonParser();

    private final Collection configCollection;
    private final JsonObject dbConnections;
    private Random random;

    @Autowired
    public QuizflexServiceImpl(@Qualifier("nerdPersistClients") Map<String, NerdPersistClient> nerdPersistClients,
                               @Qualifier("configCollection") Collection configCollection,
                               @Qualifier("dbConnections") JsonObject dbConnections) {
        this.nerdPersistClients = nerdPersistClients;
        this.configCollection = configCollection;
        this.dbConnections = dbConnections;
    }

    @PostConstruct
    public void initialize() throws JsonProcessingException {
        topicwiseQuizIds = new HashMap<>();
        subtopicwiseQuizIds = new HashMap<>();
        contentMaster = new HashMap<>();

        random = new Random();
        fetchDataFromPersist();
    }

    private void fetchDataFromPersist() throws JsonProcessingException {
        JsonArray connectionsArray = dbConnections.getArray("connections");
        com.google.gson.JsonArray gsonConnectionsArray = jsonParser.parse((connectionsArray.toString())).getAsJsonArray();

        for (int i = 0; i < gsonConnectionsArray.size(); i++) {
            com.google.gson.JsonObject currentConnection = gsonConnectionsArray.get(i).getAsJsonObject();
            String thisBucketName = currentConnection.get("bucket").getAsString();
            if (!thisBucketName.equals("content"))
                continue;

            com.google.gson.JsonArray currentScopes = currentConnection.get("scopes").getAsJsonArray();
            for (int j = 0; j < currentScopes.size(); j++) {
                com.google.gson.JsonObject currentScope = currentScopes.get(j).getAsJsonObject();
                String thisScopeName = currentScope.get("scope").getAsString();
                if (!thisScopeName.equals("quizflex"))
                    continue;

                ObjectMapper objectMapper = new ObjectMapper();
                com.google.gson.JsonArray currentCollections = currentScope.get("collections").getAsJsonArray();
                for (int k = 0; k < currentCollections.size(); k++) {
                    String thisCollectionName = currentCollections.get(k).getAsString();
                    String clientId = thisBucketName + "." + thisScopeName + "." + thisCollectionName;
                    NerdPersistClient thisPersistClient = nerdPersistClients.get(clientId);
                    if (thisPersistClient == null) {
                        System.out.println("Client id is null: " + clientId);
                        continue;
                    }

                    JsonObject collectionsToTopicMappingDoc = configCollection.get("collection_topic_mapping").contentAsObject();
                    String query = "SELECT * FROM `" + clientId.replace(".", "`.`") + "` WHERE topic_name = '" + collectionsToTopicMappingDoc.getString(thisCollectionName) + "'";
                    List<com.google.gson.JsonObject> allDocuments = thisPersistClient.getDocumentsByQuery(query, thisCollectionName);
                    for (int doc = 0; doc < allDocuments.size(); doc++) {
                        com.google.gson.JsonObject thisQuizDocument = allDocuments.get(doc);
                        QuizflexEntity quizflexEntity = objectMapper.readValue(thisQuizDocument.toString(), QuizflexEntity.class);
                        contentMaster.put(quizflexEntity.getId(), quizflexEntity);
                        List<String> currentTopicWiseIds = topicwiseQuizIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new ArrayList<>());
                        currentTopicWiseIds.add(quizflexEntity.getId());
                        topicwiseQuizIds.put(collectionsToTopicMappingDoc.getString(thisCollectionName), currentTopicWiseIds);

                        Map<String, List<String>> subtopicMap = subtopicwiseQuizIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new HashMap<>());
                        List<String> subtopicList = subtopicMap.getOrDefault(quizflexEntity.getSub_topic(), new ArrayList<>());
                        subtopicList.add(quizflexEntity.getId());
                        subtopicMap.put(quizflexEntity.getSub_topic(), subtopicList);
                        subtopicwiseQuizIds.put(collectionsToTopicMappingDoc.getString(thisCollectionName), subtopicMap);
                        System.out.println("Added topic: " + collectionsToTopicMappingDoc.getString(thisCollectionName) + ", sub topic: " + quizflexEntity.getSub_topic());
                    }
                    System.out.println("Loaded: " + allDocuments.size() +" documents for: " + clientId);
                }
            }
        }
    }

    @Override
    public List<QuizflexEntity> getQuizFlexes(String topic, String subtopic, int limit) {
        List<QuizflexEntity> responseEntities = new ArrayList<>();
        System.out.println("topic: " + topic + ", subtopic: " + subtopic + ", limit: " + limit);
        List<String> responseQuizflexIds = subtopic.equalsIgnoreCase("random") ? getRandomQuizflexIds(limit, topicwiseQuizIds.get(topic)) : getRandomQuizflexIds(limit, subtopicwiseQuizIds.get(topic).get(subtopic));
        for(int i = 0; i < responseQuizflexIds.size(); i ++) {
            System.out.println("current id: " + responseQuizflexIds.get(i));
            responseEntities.add(contentMaster.get(responseQuizflexIds.get(i)));
        }
        return responseEntities;
    }

    private List<String> getRandomQuizflexIds(int limit, List<String> idsToChooseFrom) {
        List<String> randomIds = new ArrayList<>();
        for(int i = 0; i < limit; i ++) {
            randomIds.add(idsToChooseFrom.get(getRandomIndex(idsToChooseFrom.size())));
        }
        return randomIds;
    }

    private int getRandomIndex(int capacity) {
        return random.nextInt(capacity);
    }

    @Override
    public QuizflexEntity getQuizFlexById(String id) throws Exception {
        if(contentMaster.containsKey(id))
            return contentMaster.get(id);

        throw new Exception("Invalid Quizflex Id");
    }

    /*@Scheduled(fixedRate = 1000)
    public void refreshNow() {
        System.out.println("Refreshed at: " + new Date());
    }*/
}