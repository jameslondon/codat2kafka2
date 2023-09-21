package kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import event.DataEventSubscriber;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class KafkaProducerWrapper implements AutoCloseable, DataEventSubscriber {
    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;

    public KafkaProducerWrapper() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        this.producer = new KafkaProducer<String, String>(props);
        this.objectMapper = new ObjectMapper();
    }

    public void produceToKafka(String topic, List<Map<String, Object>> records) throws Exception {
        //this will send topic message on each record level
        for (Map<String, Object> record : records) {
            String jsonData = objectMapper.writeValueAsString(record);
            producer.send(new ProducerRecord<String, String>(topic, jsonData));
        }
    }

    public void produceStringMessageToKafka(String topic, String message) throws Exception {
        // Generate correlationId
//        String correlationId = UUID.randomUUID().toString();
        //need to retrieve the apiKey from a secured vault such as HashCorp Vault or Kubernetes Secrets
        //also we need to get a valid authorization token for authorization header
//        String apiKey = "apiKey-bd6fb818-ac13-41ee-9848-b01e771eeb64";
//        Headers headers = new RecordHeaders();
//        headers.add(new RecordHeader("correlationId", correlationId.getBytes()));
//        headers.add(new RecordHeader("apiKey", apiKey.getBytes()));
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, "", message);
        producer.send(record);
    }

    @Override
    public void close() {
        producer.close();
    }

    @Override
    public void onDataFetched(String rowData, String serviceName) throws NoSuchAlgorithmException {
        //create a topic for the service
        String topic = Character.toUpperCase(serviceName.charAt(0)) + serviceName.substring(1) + "_RAW";
        try {
            produceStringMessageToKafka(topic, rowData);
            System.out.println("Kafka producer produces - Topic: " + topic + "--- Message: " + rowData);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
