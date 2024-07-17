package oracle.jms.jfr.collector.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JFRRecordingRequest {
    @JsonProperty("payload")
    private JFRRecordingRequestPayload payload;
    private String outputFileName;
    private RequestStatusEnum status;

    public JFRRecordingRequest() {
        // Default constructor for jackson
    }

    public JFRRecordingRequest(JFRRecordingRequestPayload payload) {
        this.payload = payload;
    }

    static public JFRRecordingRequest fromJsonPayload(String jsonString) {
        ObjectMapper mapper = new ObjectMapper();
        JFRRecordingRequestPayload newPayload;
        try {
            newPayload = mapper.readValue(jsonString, JFRRecordingRequestPayload.class);
        } catch (JsonProcessingException e) {
            return null;
        }
        return new JFRRecordingRequest(newPayload);
    }

    public JFRRecordingRequestPayload getPayload() {
        return payload;
    }

    public void setPayload(JFRRecordingRequestPayload payload) {
        this.payload = payload;
    }

    public RequestStatusEnum getStatus() {
        return status;
    }

    public void setStatus(RequestStatusEnum status) {
        this.status = status;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    public void generateOutputFileName() {
        if ((payload.getIp() == null) || (payload.getPort() == null) || (payload.getNumberOfSeconds() == 0)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss.SSS");
        String timeNow = now.format(formatter);
        this.outputFileName =
                payload.getIp() + "-" + payload.getPort() + "-" + timeNow + "-" + payload.getNumberOfSeconds() + "sec.jfr";
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

    public void setRequestStatus(RequestStatusEnum statusEnum) {
        this.status = statusEnum;
    }
}
