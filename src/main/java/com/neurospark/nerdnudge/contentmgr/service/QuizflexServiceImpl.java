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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class QuizflexServiceImpl implements QuizflexService {

    private final Map<String, NerdPersistClient> nerdPersistClients;
    private Map<String, Map<String, List<String>>> topicwiseQuizIds;
    private Map<String, List<String>> topicwiseNerdshotIds;
    private Map<String, Map<String, Map<String, List<String>>>> subtopicwiseQuizIds;
    private Map<String, Map<String, List<String>>> subtopicwiseNerdshotIds;
    private Map<String, QuizflexEntity> contentMaster;

    private final JsonParser jsonParser = new JsonParser();

    private final Collection configCollection;
    private final JsonObject dbConnections;
    private Random random;
    private NerdPersistClient shotsStatsPersist;
    private Map<String, Map<String, Integer>> userLevelDifficultyPercentConfig;

    private static final String LIKES_SUFFIX = "-Likes";
    private static final String DISLIKES_SUFFIX = "-Dislikes";
    private static final String FAVS_SUFFIX = "-Favs";
    private static final String SHARES_SUFFIX = "-Shares";

    @Autowired
    private TopicsServiceImpl topicsService;

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
            topicwiseNerdshotIds = new HashMap<>();
            subtopicwiseQuizIds = new HashMap<>();
            subtopicwiseNerdshotIds = new HashMap<>();
            contentMaster = new HashMap<>();

            random = new Random();
            fetchQuizflexDataFromPersist("quizflex", topicwiseQuizIds, subtopicwiseQuizIds);
            fetchShotsDataFromPersist("nerdshots", topicwiseNerdshotIds, subtopicwiseNerdshotIds);
            updateUserDifficultyLevelsConfigCache();
        } catch (Exception e) {
            log.error("Error during initialization: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateUserDifficultyLevelsConfigCache() {
        userLevelDifficultyPercentConfig = new HashMap<>();
        JsonObject nerdConfigObject = configCollection.get("nerd_config").contentAsObject();
        JsonObject userLevelsObject = nerdConfigObject.getObject("userLevels");
        Set<String> keySet = userLevelsObject.getNames();
        Iterator<String> userLevelsIterator = keySet.iterator();
        while(userLevelsIterator.hasNext()) {
            String thisLevel = userLevelsIterator.next();
            JsonObject thisLevelObject = userLevelsObject.getObject(thisLevel);
            JsonObject thisLevelDiffPercentObject = thisLevelObject.getObject("difficultyPercent");
            Map<String, Integer> thisLevelDiffMap = new HashMap<>();
            thisLevelDiffMap.put("Easy", thisLevelDiffPercentObject.containsKey("Easy") ? thisLevelDiffPercentObject.getInt("Easy") : 0);
            thisLevelDiffMap.put("Medium", thisLevelDiffPercentObject.containsKey("Medium") ? thisLevelDiffPercentObject.getInt("Medium") : 0);
            thisLevelDiffMap.put("Hard", thisLevelDiffPercentObject.containsKey("Hard") ? thisLevelDiffPercentObject.getInt("Hard") : 0);
            userLevelDifficultyPercentConfig.put(thisLevel, thisLevelDiffMap);
        }
        log.info("user difficulty levels config cache: {}", userLevelDifficultyPercentConfig);
    }

    private void fetchQuizflexDataFromPersist(String type, Map<String, Map<String, List<String>>> topicwiseIds, Map<String, Map<String, Map<String, List<String>>>> subtopicwiseIds) throws JsonProcessingException {
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
                if (!thisScopeName.equals(type))
                    continue;

                ObjectMapper objectMapper = new ObjectMapper();
                com.google.gson.JsonArray currentCollections = currentScope.get("collections").getAsJsonArray();
                for (int k = 0; k < currentCollections.size(); k++) {
                    String thisCollectionName = currentCollections.get(k).getAsString();
                    String schemaId = thisBucketName + "." + thisScopeName + "." + thisCollectionName;
                    NerdPersistClient thisPersistClient = nerdPersistClients.get(schemaId);
                    if (thisPersistClient == null) {
                        log.warn("Schema id does not exist: {}", schemaId);
                        continue;
                    }

                    JsonObject collectionsToTopicMappingDoc = configCollection.get("collection_topic_mapping").contentAsObject();
                    String topicName = collectionsToTopicMappingDoc.getString(thisCollectionName);
                    String query = "SELECT * FROM `" + schemaId.replace(".", "`.`") + "` WHERE topic_name = '" + topicName + "'";
                    List<com.google.gson.JsonObject> allDocuments = thisPersistClient.getDocumentsByQuery(query, thisCollectionName);
                    for (int doc = 0; doc < allDocuments.size(); doc++) {
                        com.google.gson.JsonObject thisQuizDocument = allDocuments.get(doc);
                        QuizflexEntity quizflexEntity = objectMapper.readValue(thisQuizDocument.toString(), QuizflexEntity.class);
                        contentMaster.put(quizflexEntity.getId(), quizflexEntity);
                        Map<String, List<String>> topicWiseDifficultyMap = topicwiseIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new HashMap<>());
                        String currentDifficultyLevel = quizflexEntity.getDifficulty_level();
                        List<String> difficultyLevelIds = topicWiseDifficultyMap.getOrDefault(currentDifficultyLevel, new ArrayList<>());
                        difficultyLevelIds.add(quizflexEntity.getId());
                        topicWiseDifficultyMap.put(currentDifficultyLevel, difficultyLevelIds);
                        topicwiseIds.put(topicName, topicWiseDifficultyMap);

                        if(subtopicwiseIds != null) {
                            Map<String, Map<String, List<String>>> subtopicMap = subtopicwiseIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new HashMap<>());
                            Map<String, List<String>> subtopicDifficultyMap = subtopicMap.getOrDefault(quizflexEntity.getSub_topic(), new HashMap<>());
                            List<String> subtopicList = subtopicDifficultyMap.getOrDefault(currentDifficultyLevel, new ArrayList<>());

                            subtopicList.add(quizflexEntity.getId());
                            subtopicDifficultyMap.put(currentDifficultyLevel, subtopicList);
                            subtopicMap.put(quizflexEntity.getSub_topic(), subtopicDifficultyMap);
                            subtopicwiseIds.put(topicName, subtopicMap);
                            //log.info("Added topic: {}, sub topic: {}", topicName, quizflexEntity.getSub_topic());
                        }
                    }

                    Map<String, Map<String, List<String>>> topicsMap = subtopicwiseIds.get(topicName);
                    for (Map.Entry<String, Map<String, List<String>>> subtopicEntry : topicsMap.entrySet()) {
                        String subtopicName = subtopicEntry.getKey();
                        Map<String, List<String>> innerMap = subtopicEntry.getValue();

                        log.info("Topic: {}. Subtopic: {}", topicName, subtopicName);

                        for (Map.Entry<String, List<String>> innerEntry : innerMap.entrySet()) {
                            String innerKey = innerEntry.getKey();
                            int size = innerEntry.getValue().size();
                            log.info("   {} -> {}", innerKey, size);
                        }
                    }

                    log.info("Loaded: {} documents for: {}", allDocuments.size(), schemaId);
                }
            }
        }
    }

    private void fetchShotsDataFromPersist(String type, Map<String, List<String>> topicwiseIds, Map<String, Map<String, List<String>>> subtopicwiseIds) throws JsonProcessingException {
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
                if (!thisScopeName.equals(type))
                    continue;

                ObjectMapper objectMapper = new ObjectMapper();
                com.google.gson.JsonArray currentCollections = currentScope.get("collections").getAsJsonArray();
                for (int k = 0; k < currentCollections.size(); k++) {
                    String thisCollectionName = currentCollections.get(k).getAsString();
                    String schemaId = thisBucketName + "." + thisScopeName + "." + thisCollectionName;
                    NerdPersistClient thisPersistClient = nerdPersistClients.get(schemaId);
                    if (thisPersistClient == null) {
                        log.warn("Schema id does not exist: {}", schemaId);
                        continue;
                    }

                    JsonObject collectionsToTopicMappingDoc = configCollection.get("collection_topic_mapping").contentAsObject();
                    String query = "SELECT * FROM `" + schemaId.replace(".", "`.`") + "` WHERE topic_name = '" + collectionsToTopicMappingDoc.getString(thisCollectionName) + "'";
                    List<com.google.gson.JsonObject> allDocuments = thisPersistClient.getDocumentsByQuery(query, thisCollectionName);
                    for (int doc = 0; doc < allDocuments.size(); doc++) {
                        com.google.gson.JsonObject thisQuizDocument = allDocuments.get(doc);
                        QuizflexEntity quizflexEntity = objectMapper.readValue(thisQuizDocument.toString(), QuizflexEntity.class);
                        contentMaster.put(quizflexEntity.getId(), quizflexEntity);
                        List<String> currentTopicWiseIds = topicwiseIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new ArrayList<>());
                        currentTopicWiseIds.add(quizflexEntity.getId());
                        topicwiseIds.put(collectionsToTopicMappingDoc.getString(thisCollectionName), currentTopicWiseIds);

                        if(subtopicwiseIds != null) {
                            Map<String, List<String>> subtopicMap = subtopicwiseIds.getOrDefault(collectionsToTopicMappingDoc.getString(thisCollectionName), new HashMap<>());
                            List<String> subtopicList = subtopicMap.getOrDefault(quizflexEntity.getSub_topic(), new ArrayList<>());
                            subtopicList.add(quizflexEntity.getId());
                            subtopicMap.put(quizflexEntity.getSub_topic(), subtopicList);
                            subtopicwiseIds.put(collectionsToTopicMappingDoc.getString(thisCollectionName), subtopicMap);
                            //log.info("Added topic: {}, sub topic: {}", collectionsToTopicMappingDoc.getString(thisCollectionName), quizflexEntity.getSub_topic());
                        }
                    }
                    log.info("Loaded: {} documents for: {}", allDocuments.size(), schemaId);
                }
            }
        }
    }

    @Override
    public List<QuizflexEntity> getQuizFlexes(String topic, String subtopic, int limit, String userId) throws Exception {
        List<QuizflexEntity> responseEntities = new ArrayList<>();

        log.info("Getting Quizflex: topic: {}, subtopic: {}, limit: {}", topic, subtopic, limit);
        Map<String, String> userSubtopicLevels = topicsService.getUserSubtopicLevels(topic, userId);
        String userSubtopicLevel = userSubtopicLevels.getOrDefault(subtopic, "Novice");
        log.info("");
        List<String> responseQuizflexIds = subtopic.equalsIgnoreCase("random") ? getRandomQuizflexIds(limit, userSubtopicLevel, topicwiseQuizIds.get(topic)) : getRandomQuizflexIds(limit, userSubtopicLevel, subtopicwiseQuizIds.get(topic).get(subtopic));
        for(int i = 0; i < responseQuizflexIds.size(); i ++) {
            QuizflexEntity thisQuizflex = getQuizFlex(responseQuizflexIds.get(i));
            log.info("Adding QF: {}", thisQuizflex);
            responseEntities.add(thisQuizflex);
        }
        return responseEntities;
    }



    private List<String> getRandomQuizflexIds(int limit, String userSubtopicLevel, Map<String, List<String>> difficultyBasedidsToChooseFrom) {

        List<String> randomIds = new ArrayList<>();
        Map<String, Integer> difficultyPercent = userLevelDifficultyPercentConfig.get(userSubtopicLevel);
        log.info("User subtopic level: {}", userSubtopicLevel);

        int easyCount = (int) Math.round(limit * (difficultyPercent.getOrDefault("Easy", 0) / 100.0));
        int mediumCount = (int) Math.round(limit * (difficultyPercent.getOrDefault("Medium", 0) / 100.0));
        int hardCount = (int) Math.round(limit * (difficultyPercent.getOrDefault("Hard", 0) / 100.0));

        int totalPicked = easyCount + mediumCount + hardCount;
        while (totalPicked < limit) {
            easyCount++;
            totalPicked++;
        }

        log.info("Counts to add based on difficulty: {}, {}, {}", easyCount, mediumCount, hardCount);

        if (easyCount > 0 && difficultyBasedidsToChooseFrom.containsKey("Easy")) {
            randomIds.addAll(getRandomFromList(difficultyBasedidsToChooseFrom.get("Easy"), easyCount));
        }

        if (mediumCount > 0 && difficultyBasedidsToChooseFrom.containsKey("Medium")) {
            randomIds.addAll(getRandomFromList(difficultyBasedidsToChooseFrom.get("Medium"), mediumCount));
        }

        if (hardCount > 0 && difficultyBasedidsToChooseFrom.containsKey("Hard")) {
            randomIds.addAll(getRandomFromList(difficultyBasedidsToChooseFrom.get("Hard"), hardCount));
        }

        Collections.shuffle(randomIds);
        log.info("Total random Ids added: {}", randomIds.size());
        return randomIds;
    }

    private List<String> getRandomFromList(List<String> source, int count) {
        List<String> randomIds = new ArrayList<>();
        for(int i = 0; i < count; i ++) {
            randomIds.add(source.get(getRandomIndex(source.size())));
        }
        return randomIds;
    }

    @Override
    public List<QuizflexEntity> getNerdShots(String topic, String subtopic, int limit) throws Exception {
        List<QuizflexEntity> responseEntities = new ArrayList<>();

        log.info("Getting NerdShot: topic: {}, subtopic: {}, limit: {}", topic, subtopic, limit);
        List<String> responseQuizflexIds = subtopic.equalsIgnoreCase("random") ? getRandomShotsIds(limit, topicwiseNerdshotIds.get(topic)) : getRandomShotsIds(limit, subtopicwiseNerdshotIds.get(topic).get(subtopic));
        for(int i = 0; i < responseQuizflexIds.size(); i ++) {
            responseEntities.add(getQuizFlex(responseQuizflexIds.get(i)));
        }
        return responseEntities;
    }

    private List<String> getRandomShotsIds(int limit, List<String> idsToChooseFrom) {
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
            return null;

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
            QuizflexEntity thisResult = getQuizFlex(ids.get(i).getAsString());
            if(thisResult != null)
                result.add(thisResult);
        }
        return result;
    }
}