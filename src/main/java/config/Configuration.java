package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
public class Configuration {
    private static Configuration config;
    private String  clientId;
    private String  username;
    private String  nCinoPrivateKeyPath;
    private String tokenEndpoint;
    private String nCinoInstanceUrl;
    private Long relayFrom;
    private String gcsBuckName;
    private String bigQueryDatasetName;
    private String subscribedChangeEvents;

    private String jwtKeystoreType;
    private String jwtKeystorePath;
    private String jwtKeystoreAlias;
    private String sfKeystoreType;
    private String sfKeystorePath;
    private String sfKeystoreAlias;
    private String googleCredentialKeyPath;

    public String getClientId() {
        return clientId;
    }
    public String getUsername() {
        return username;
    }
    public String getnCinoPrivateKeyPath() { return nCinoPrivateKeyPath; }
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }
    public long getRelayFrom() { return relayFrom; }
    public String getSubscribedChangeEvents( ) { return subscribedChangeEvents; }

    public String getGcsBuckName() {return gcsBuckName;}
    public String getBigQueryDatasetName() {return bigQueryDatasetName;}
    public String getNCinoInstanceUrl() {return nCinoInstanceUrl;}

    public String getJwtKeystoreType() {
        return jwtKeystoreType;
    }
    public String getJwtKeystorePath() {return jwtKeystorePath;}
    public String getJwtKeystoreAlias() {return jwtKeystoreAlias;}
    public String getSfKeystoreType() {
        return sfKeystoreType;
    }
    public String getSfKeystorePath() {return sfKeystorePath;}
    public String getSfKeystoreAlias() {return sfKeystoreAlias;}
    public String getGoogleCredentialKeyPath() {return googleCredentialKeyPath;}
    public static Configuration get() {
        return config;
    }
    public static Configuration get(String propertyFile) {
        if (config == null) {
            try (InputStream input = new FileInputStream(propertyFile)) {
                Properties prop = new Properties();
                prop.load(input);
                config = new Configuration();
                config.clientId = readMandatoryProp(prop, "clientId");
                config.username = readMandatoryProp(prop, "username");
                config.nCinoPrivateKeyPath = readOptionalProp(prop, "nCinoPrivateKeyPath");
                config.tokenEndpoint = readMandatoryProp(prop, "tokenEndpoint");
                config.nCinoInstanceUrl = readMandatoryProp(prop, "nCinoInstanceUrl");
                config.subscribedChangeEvents = readMandatoryProp(prop, "subscribedChangeEvents");
                config.gcsBuckName = readMandatoryProp(prop, "gcsBuckName");
                config.bigQueryDatasetName = readMandatoryProp(prop, "bigQueryDatasetName");
                config.jwtKeystoreType = readMandatoryProp(prop, "jwtKeystoreType");
                config.jwtKeystorePath = readMandatoryProp(prop, "jwtKeystorePath");
                config.jwtKeystoreAlias = readMandatoryProp(prop, "jwtKeystoreAlias");
                config.sfKeystoreType = readMandatoryProp(prop, "sfKeystoreType");
                config.sfKeystorePath = readMandatoryProp(prop, "sfKeystorePath");
                config.sfKeystoreAlias = readMandatoryProp(prop, "sfKeystoreAlias");
                config.googleCredentialKeyPath = readOptionalProp(prop, "googleCredentialKeyPath");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load configuration: " + e.getMessage(), e);
            }
        }
        return config;
    }

    private static String readMandatoryProp(Properties prop, String key) throws IOException {
        String value = prop.getProperty(key);
        if (value == null || value.trim().equals("")) {
            throw new IOException("Missing mandatory property: " + key);
        }
        return value;
    }

    private static int readMantoryIntProp(Properties prop, String key) throws IOException {
        String stringValue = readMandatoryProp(prop, key);
        return Integer.valueOf(stringValue);
    }
    private static String readOptionalProp(Properties prop, String key) throws IOException {
        return prop.getProperty(key);
    }
}

