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
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QuizflexServiceImpl implements QuizflexService {

    private final Map<String, NerdPersistClient> nerdPersistClients;
    private Map<String, List<String>> topicwiseQuizIds;
    private Map<String, List<String>> topicwiseRWCIds;
    private Map<String, Map<String, List<String>>> subtopicwiseQuizIds;
    private Map<String, QuizflexEntity> contentMaster;

    private final JsonParser jsonParser = new JsonParser();

    private final Collection configCollection;
    private final JsonObject dbConnections;
    private Random random;
    private NerdPersistClient shotsStatsPersist;

    private static final String LIKES_SUFFIX = "-Likes";
    private static final String DISLIKES_SUFFIX = "-Dislikes";
    private static final String FAVS_SUFFIX = "-Favs";
    private static final String SHARES_SUFFIX = "-Shares";

    @Autowired
    public QuizflexServiceImpl(@Qualifier("nerdPersistClients") Map<String, NerdPersistClient> nerdPersistClients,
                               @Qualifier("configCollection") Collection configCollection,
                               @Qualifier("dbConnections") JsonObject dbConnections,
                               @Qualifier("shotsStatsPersist") NerdPersistClient shotsStatsPersist) {
        this.nerdPersistClients = nerdPersistClients;
        this.configCollection = configCollection;
        this.dbConnections = dbConnections;
        this.shotsStatsPersist = shotsStatsPersist;
    }

    @PostConstruct
    public void initialize() {
        try {
            topicwiseQuizIds = new HashMap<>();
            subtopicwiseQuizIds = new HashMap<>();
            contentMaster = new HashMap<>();

            random = new Random();
            fetchQuizflexDataFromPersist();
            fetchRWCDataFromPersist();
        } catch (Exception e) {
            System.err.println("Error during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fetchQuizflexDataFromPersist() throws JsonProcessingException {
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

    private void fetchRWCDataFromPersist() throws JsonProcessingException {
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
                if (!thisScopeName.equals("rwc"))
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
                        List<String> currentTopicWiseIds = topicwiseRWCIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new ArrayList<>());
                        currentTopicWiseIds.add(quizflexEntity.getId());
                        topicwiseRWCIds.put(collectionsToTopicMappingDoc.getString(thisCollectionName), currentTopicWiseIds);

                        System.out.println("RWC: Added topic: " + collectionsToTopicMappingDoc.getString(thisCollectionName) + ", sub topic: " + quizflexEntity.getSub_topic());
                    }
                    System.out.println("Loaded: " + allDocuments.size() +" documents for: " + clientId);
                }
            }
        }
    }

    @Override
    public List<QuizflexEntity> getQuizFlexes(String topic, String subtopic, int limit) throws Exception {
        List<QuizflexEntity> responseEntities = new ArrayList<>();
        System.out.println("topic: " + topic + ", subtopic: " + subtopic + ", limit: " + limit);
        List<String> responseQuizflexIds = subtopic.equalsIgnoreCase("random") ? getRandomQuizflexIds(limit, topicwiseQuizIds.get(topic)) : getRandomQuizflexIds(limit, subtopicwiseQuizIds.get(topic).get(subtopic));
        for(int i = 0; i < responseQuizflexIds.size(); i ++) {
            System.out.println("current id: " + responseQuizflexIds.get(i));
            responseEntities.add(getQuizFlex(responseQuizflexIds.get(i)));
        }
        return responseEntities;
    }

    @Override
    public List<QuizflexEntity> getRealworldChallenge(String topic, String subtopic, int limit) throws Exception {
        List<QuizflexEntity> responseEntities = new ArrayList<>();
        System.out.println("RWC: topic: " + topic + ", subtopic: " + subtopic + ", limit: " + limit);
        List<String> responseQuizflexIds = getRandomQuizflexIds(limit, topicwiseRWCIds.get(topic));
        for(int i = 0; i < responseQuizflexIds.size(); i ++) {
            System.out.println("current id: " + responseQuizflexIds.get(i));
            responseEntities.add(getQuizFlex(responseQuizflexIds.get(i)));
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

    private QuizflexEntity getQuizFlex(String id) throws Exception {
        if(! contentMaster.containsKey(id))
            throw new Exception("Invalid Quizflex Id");

        QuizflexEntity thisQuizFlex = contentMaster.get(id);
        thisQuizFlex.setLikes(shotsStatsPersist.getCounter(id + LIKES_SUFFIX));
        thisQuizFlex.setDislikes(shotsStatsPersist.getCounter(id + DISLIKES_SUFFIX));
        thisQuizFlex.setFavorites(shotsStatsPersist.getCounter(id + FAVS_SUFFIX));
        thisQuizFlex.setShares(shotsStatsPersist.getCounter(id + SHARES_SUFFIX));

        return thisQuizFlex;
    }

    @Override
    public QuizflexEntity getQuizflexById(String id) throws Exception {
        return getQuizFlex(id);
    }

    @Override
    public List<QuizflexEntity> getFavoriteQuizflexesByIds(com.google.gson.JsonArray ids) throws Exception {
        List<QuizflexEntity> result = new ArrayList<>();
        for(int i = 0; i < ids.size(); i ++) {
            result.add(getQuizFlex(ids.get(i).getAsString()));
        }
        return result;
    }
}