package auth;

public class ServiceAuthDetails {
    private final String serviceName;
    private final String apiKey;
    private final String authorization;

    public ServiceAuthDetails(String serviceName, String apiKey, String authorization) {
        this.serviceName = serviceName;
        this.apiKey = apiKey;
        this.authorization = authorization;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAuthorization() {
        return authorization;
    }
}
