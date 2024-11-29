package com.neurospark.nerdnudge.persist.config;

import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.google.gson.JsonParser;
import com.neurospark.nerdnudge.couchbase.service.NerdPersistClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Component
public class NerdPersistConfig {
    private final PersistClusterConfig persistClusterConfig;
    private final JsonParser jsonParser = new JsonParser();

    @Autowired
    public NerdPersistConfig(PersistClusterConfig persistClusterConfig) {
        this.persistClusterConfig = persistClusterConfig;
    }

    @Bean
    public Map<String, NerdPersistClient> nerdPersistClients(@Qualifier("dbConnections") JsonObject dbConnections) {
        Map<String, NerdPersistClient> clients = new HashMap<>();

        JsonArray connectionsArray = dbConnections.getArray("connections");
        com.google.gson.JsonArray gsonConnectionsArray = jsonParser.parse((connectionsArray.toString())).getAsJsonArray();

        for(int i = 0; i < gsonConnectionsArray.size(); i ++) {
            com.google.gson.JsonObject currentConnection = gsonConnectionsArray.get(i).getAsJsonObject();
            String thisBucketName = currentConnection.get("bucket").getAsString();
            com.google.gson.JsonArray currentScopes = currentConnection.get("scopes").getAsJsonArray();
            for(int j = 0; j < currentScopes.size(); j ++) {
                com.google.gson.JsonObject currentScope = currentScopes.get(j).getAsJsonObject();
                String thisScopeName = currentScope.get("scope").getAsString();
                com.google.gson.JsonArray currentCollections = currentScope.get("collections").getAsJsonArray();
                for(int k = 0; k < currentCollections.size(); k ++) {
                    String thisCollectionName = currentCollections.get(k).getAsString();
                    clients.put(thisBucketName + "." + thisScopeName + "." + thisCollectionName, new NerdPersistClient(persistClusterConfig.getPersistConnectionString(), persistClusterConfig.getPersistUsername(), persistClusterConfig.getPersistPassword(), thisBucketName, thisScopeName, thisCollectionName));
                }
            }
        }
        return clients;
    }
}