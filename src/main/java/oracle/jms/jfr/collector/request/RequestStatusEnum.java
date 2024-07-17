package oracle.jms.jfr.collector.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum RequestStatusEnum {
    @JsonProperty("PENDING") PENDING, @JsonProperty("COLLECTING") COLLECTING, @JsonProperty(
            "UPLOADING") UPLOADING, @JsonProperty("DONE") DONE, @JsonProperty("ERROR") ERROR
}
