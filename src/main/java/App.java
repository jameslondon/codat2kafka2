import auth.ServiceAuthDetails;
import client.CodatClient;
import kafka.KafkaProducerWrapper;
import util.ResponseHasher;
import util.ResponseHasherUUID;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class App {
    private static final int NUMBER_OF_THREADS = 40;
    private static final ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static void main(String[] args) throws Exception {
        try (KafkaProducerWrapper kafkaProducer = new KafkaProducerWrapper()) {
            // load previously saved response payload hash set
//            ResponseHasher.loadHashSetFromFile();
            // use Redis to store response payload hash and UUID
            ResponseHasherUUID.connectToRedis();

            CodatClient client = new CodatClient();
            // Register / subscribe KafkaProducerWrapper as the data event listener/publisher for CodatClient
            client.setDataEventSubscriber(kafkaProducer);

            Properties serviceProperties = loadProperties("config/CompanyServiceConfig.properties");
            Properties authProperties = loadProperties("config/CompanyAuthConfig.properties");
            Properties dataPullProperties = loadProperties("config/DataPullConfig.properties");
            Properties serviceGroupProperties = loadProperties("config/ServiceGroupConfig.properties");
            Properties CompanyConnectionProperties = loadProperties("config/CompanyConnectionConfig.properties");

            Set<String> connectionServicesSet = new HashSet<>(Arrays.asList(
                    serviceGroupProperties.getProperty("COMPANYID_CONNECTIONID_SERVICENAMES").split(","))
            );

            processServiceProperties(serviceProperties, connectionServicesSet, dataPullProperties, authProperties, CompanyConnectionProperties, client);

        } catch (IOException ioException) {
            System.out.println("Encountered an IO issue: " + ioException.getMessage());
        } catch (Exception e) {
            System.out.println("Oops!: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //save updated/add response payload hash set
//        ResponseHasher.saveHashSetToFile();
        // close Redis
        ResponseHasherUUID.closeRedis();
        // shut down the thread pool
        executorService.shutdown();
    }

    private static Properties loadProperties(String fileName) throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = App.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        }
        return properties;
    }

    private static void processServiceProperties(Properties serviceProperties,
                                                 Set<String> connectionServicesSet,
                                                 Properties dataPullProperties,
                                                 Properties authProperties,
                                                 Properties CompanyConnectionProperties,
                                                 CodatClient client) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        serviceProperties.stringPropertyNames().forEach(companyId -> {
            String[] services = serviceProperties.getProperty(companyId).split(",");
            Stream.of(services).forEach(serviceName -> {
                List<AbstractMap.SimpleEntry<String, ServiceAuthDetails>> entries = new ArrayList<>();
                if (connectionServicesSet.contains(serviceName)) {
                    String[] connectionIds = CompanyConnectionProperties.getProperty(companyId).split(",");
                    for (String connectionId : connectionIds) {
                        String baseApiUrl = String.format(dataPullProperties.getProperty("API_URL_COMPANYID_CONNECTIONID_SERVICENAME"), companyId, connectionId, serviceName);
                        ServiceAuthDetails authDetails = new ServiceAuthDetails(serviceName, authProperties.getProperty(companyId + "-apiKey"), authProperties.getProperty(companyId + "-Authorization"));
                        entries.add(new AbstractMap.SimpleEntry<>(baseApiUrl, authDetails));
                    }
                } else {
                    String baseApiUrl = String.format(dataPullProperties.getProperty("API_URL_COMPANYID_SERVICENAME"), companyId, serviceName);
                    ServiceAuthDetails authDetails = new ServiceAuthDetails(serviceName, authProperties.getProperty(companyId + "-apiKey"), authProperties.getProperty(companyId + "-Authorization"));
                    entries.add(new AbstractMap.SimpleEntry<>(baseApiUrl, authDetails));
                }

                entries.forEach(entry -> {
                    CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            client.fetchAllDataString(entry.getKey(),
                                    entry.getValue().getServiceName(),
                                    entry.getValue().getApiKey(),
                                    entry.getValue().getAuthorization(),
                                    dataPullProperties,
                                    executorService);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    }, executorService);

                    futures.add(future);
                });
            });
        });

        // Wait for all futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();
    }
}
