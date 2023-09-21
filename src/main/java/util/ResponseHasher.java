package util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ResponseHasher {
    private static final String HASHED_RESPONSE_LIST_FILE = "/persistent/hashedResponseList.log";
    private static Set<String> hashSet = new HashSet<>();
    public static Predicate<String> isDuplicate = (hash) -> {
        boolean isDuplicate = hashSet.contains(hash);
        if (!isDuplicate) {
            hashSet.add(hash);
            System.out.println("It is a new hash: " + hash);
        } else {
            System.out.println("Hash is a duplicate and is not added: " + hash);
        }
        return isDuplicate;
    };

    public static void loadHashSetFromFile() {
        try (InputStream inputStream = ResponseHasher.class.getResourceAsStream(HASHED_RESPONSE_LIST_FILE);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                hashSet.add(line);
            }
            System.out.println("Hash set loaded as **********************: " + hashSet);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void saveHashSetToFile() {
        try (OutputStream outputStream = new FileOutputStream(ResponseHasher.class.getResource(HASHED_RESPONSE_LIST_FILE).getPath());
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            for (String hash : hashSet) {
                writer.write(hash);
                writer.newLine();
            }
            System.out.println("Hash set saved as **********************: " + hashSet);
        } catch (IOException e) {
            // Handle file saving error
            System.out.println(e.getMessage());
        }
    }

    public static String calculateHash(String response) throws NoSuchAlgorithmException {
        // Create a SHA-256 hash object
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        // Parse the JSON response
        Gson gson = new Gson();
        JsonElement payload = gson.fromJson(response, JsonElement.class);

        // Hash the first element in the "results" list
        JsonElement firstResult = payload.getAsJsonObject().getAsJsonArray("results").get(0);
        digest.update(firstResult.toString().getBytes());

        // Hash the actual size of elements in the "results" list
        int size = payload.getAsJsonObject().getAsJsonArray("results").size();
        digest.update(Integer.toString(size).getBytes());

        if (size - 1 > 0)  {
            // Hash the last element in the "results" list
            JsonElement lastResult = payload.getAsJsonObject().getAsJsonArray("results").get(size - 1);
            digest.update(lastResult.toString().getBytes());
        }

        // Hash the "totalResults" field
        int totalResults = payload.getAsJsonObject().get("totalResults").getAsInt();
        digest.update(Integer.toString(totalResults).getBytes());

        // Hash the "_links" field
        JsonObject links = payload.getAsJsonObject().getAsJsonObject("_links");
        digest.update(links.toString().getBytes());

        // Get the final hash value
        byte[] hashBytes = digest.digest();
        StringBuilder hashBuilder = new StringBuilder();
        for (byte b : hashBytes) {
            hashBuilder.append(String.format("%02x", b));
        }
        return hashBuilder.toString();
    }
}