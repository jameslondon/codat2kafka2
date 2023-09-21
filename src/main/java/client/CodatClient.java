package client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import event.DataEventSubscriber;
import okhttp3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static util.ResponseHasherUUID.*;

public class CodatClient {
    private final OkHttpClient client;
    private DataEventSubscriber subscriber;

    public CodatClient() {
        this.client = new OkHttpClient().newBuilder()
                .build();
    }

    public void setDataEventSubscriber(DataEventSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    public CompletableFuture<Void> fetchAllDataStringAsync(String baseApiUrl, String serviceName, String apiKey, String authorization, Properties authProperties) {
        return CompletableFuture.runAsync(() -> {
            try {
                fetchAllDataString(baseApiUrl, serviceName, apiKey, authorization, authProperties);
            } catch (Exception e) {
                System.out.println("Exception in fetchAllDataString: "
                        + ". ServiceName: " + serviceName
                        + ". Message: " + e.getMessage());
            }
        });
    }

    public void fetchAllDataString(String baseApiUrl, String serviceName, String apiKey, String authorization, Properties authProperties) throws Exception {
        int pageNumber = Integer.parseInt(authProperties.getProperty("pageNumber"));
        int pageSize = Integer.parseInt(authProperties.getProperty("pageSize"));
        int maxPageNumber = Integer.parseInt(authProperties.getProperty("maxPageNumber"));
        boolean hasNextPage = true;
        String respPayload = "";

        while (hasNextPage && pageNumber <= maxPageNumber) {
            String apiUrl = baseApiUrl + "?orderBy=id&pageSize=" + pageSize + "&page=" + pageNumber;
            respPayload = this.fetchApiData(apiUrl, apiKey, authorization);
            System.out.println("apiUrl: " + apiUrl);
            if (!respPayload.isEmpty() && subscriber != null) {
                //to get the current response hash value
                String hash1 = calculateHash(respPayload);
                System.out.println("codat respPayload: " + respPayload);
                System.out.println("hash1: " + hash1);
//                if (!isDuplicate.test(hash1)) {
                    subscriber.onDataFetched(respPayload, serviceName);
//                }
                pageNumber++;
            } else {
                hasNextPage = false;
            }
        }
    }

    private List<Map<String, Object>> parseJsonData(String jsonData) {
        List<Map<String, Object>> resultsList = new ArrayList<>();

        // Parse the JSON string using Gson
        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

        // Extract the "results" array
        JsonArray resultsArray = jsonObject.getAsJsonArray("results");

        // Convert each JsonObject in the results array to a Map and add to the resultsList
        for (int i = 0; i < resultsArray.size(); i++) {
            JsonObject resultObject = resultsArray.get(i).getAsJsonObject();
            Map<String, Object> resultMap = new Gson().fromJson(resultObject, Map.class);
            System.out.println("Record " + i + ": " + resultMap);
            resultsList.add(resultMap);
        }
        return resultsList;
    }

    private String fetchApiData(String apiUrl, String apiKey, String authorization) throws Exception {
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("apiKey", apiKey)
                .header("Authorization", authorization)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            return response.body().string();
        }
    }
}