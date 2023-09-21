package event;

import java.security.NoSuchAlgorithmException;

public interface DataEventSubscriber {
    void onDataFetched(String data, String topic) throws NoSuchAlgorithmException;
}
