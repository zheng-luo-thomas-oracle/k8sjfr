package oracle.jms.jfr.collector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StartRecordingRequest {
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getNumberOfSeconds() {
        return numberOfSeconds;
    }

    public void setNumberOfSeconds(int numberOfSeconds) {
        this.numberOfSeconds = numberOfSeconds;
    }

    @JsonProperty("ip")
    private String ip;
    @JsonProperty("port")
    private String port;
    @JsonProperty("numberOfSeconds")
    private int numberOfSeconds;

    public StartRecordingRequest() {
        // Default constructor
    }

    public StartRecordingRequest(String ip, String port, int numberOfSeconds) {
        this.ip = ip;
        this.port = port;
        this.numberOfSeconds = numberOfSeconds;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    static public StartRecordingRequest fromJsonString(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        StartRecordingRequest request;
        try {
            request = mapper.readValue(jsonString, StartRecordingRequest.class);
        } catch (JsonProcessingException e) {
            return null;
        }
        return request;
    }
}
