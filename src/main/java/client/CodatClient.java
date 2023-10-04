package client;

import event.DataEventSubscriber;
import okhttp3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;


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

    public void fetchAllDataString(String baseApiUrl, String serviceName, String apiKey, String authorization, Properties authProperties, ExecutorService executorService) {
        int pageNumber = Integer.parseInt(authProperties.getProperty("pageNumber"));
        int pageSize = Integer.parseInt(authProperties.getProperty("pageSize"));
        int maxPageNumber = Integer.parseInt(authProperties.getProperty("maxPageNumber"));
        AtomicBoolean hasNextPage = new AtomicBoolean(true);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger currentPage = new AtomicInteger(1);

        int chunkSize = 10; // or some other value depending on your needs

        while (hasNextPage.get() && pageNumber <= maxPageNumber) {
            List<CompletableFuture<Void>> chunkFutures = new ArrayList<>();

            for (int i = 0; i < chunkSize && pageNumber <= maxPageNumber; i++) {
                final int page = pageNumber++;

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    String apiUrl = baseApiUrl + "?orderBy=id&pageSize=" + pageSize + "&page=" + page;
                    System.out.println("Ready to fetch: " + apiUrl);
                    String respPayload;

                    try {
                        respPayload = this.fetchApiData(apiUrl, apiKey, authorization);
//                        Thread.sleep(1000 * 60);
                    } catch (Exception e) {
                        throw new RuntimeException("Exception while fetching API data for page " + page + ": " + e.getMessage());
                    }
                    JSONObject payloadJson = new JSONObject(respPayload);
                    JSONArray results = payloadJson.getJSONArray("results");

                    if (results.length() > 0 && subscriber != null) {
//                        String hash1;
                        try {
//                            hash1 = calculateHash(respPayload);
                            subscriber.onDataFetched(respPayload, serviceName);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        hasNextPage.set(false);
                    }
                }, executorService);

                chunkFutures.add(future);
            }

            // Wait for the chunk of threads to complete
            CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).join();

            // If any of the threads in the chunk found no next page, stop
            if (!hasNextPage.get()) break;
        }

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