package util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.function.Predicate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ResponseHasherUUID {
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_PASSWORD = "redis_password";
    private static final int REDIS_TIMEOUT = 5000;

    private static final String HASHED_RESPONSE_KEY = "hashed_responses";

    private static JedisPool jedisPool;

    public static void connectToRedis() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(50);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(5);
//        jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT, REDIS_PASSWORD);
        jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_TIMEOUT);
        if (testRedisConnection()) {
            System.out.println("Connected to Redis");
        } else {
            System.out.println("Connected to Redis not established");
        }
    }
    public static boolean testRedisConnection() {
        try (Jedis jedis = jedisPool.getResource()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void closeRedis() {
        jedisPool.close();
        System.out.println("Redis connection closed");
    }

    public static Predicate<String> isDuplicate = (hash) -> {
        try (Jedis jedis = jedisPool.getResource()) {
            boolean isDuplicate = jedis.hexists(HASHED_RESPONSE_KEY, hash);
            if (!isDuplicate) {
                // Generate a new UUID for the hash value
                String uuid = UUID.randomUUID().toString();
                // Save the hash-UUID pair in Redis
                jedis.hset(HASHED_RESPONSE_KEY, hash, uuid);
                System.out.println("It is a new hash: " + hash);
                System.out.println("Hash-UUID pair saved in Redis: " + hash + " - " + uuid);
            } else {
                System.out.println("Hash is a duplicate and is not added: " + hash);
            }
            return isDuplicate;
        }
    };

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
        int size = payload.getAsJsonObject().getAsJsonArray("results").size() - 1 ;
        digest.update(Integer.toString(size).getBytes());

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