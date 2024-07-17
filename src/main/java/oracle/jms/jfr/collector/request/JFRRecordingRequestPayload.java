package oracle.jms.jfr.collector.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JFRRecordingRequestPayload {
    @JsonProperty("ip")
    private String ip;
    @JsonProperty("port")
    private String port;
    @JsonProperty("numberOfSeconds")
    private int numberOfSeconds;

    public JFRRecordingRequestPayload() {
    }

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
}
