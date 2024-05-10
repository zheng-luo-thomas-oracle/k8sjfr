package oracle.jms.jfr.collector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StartRecordingRequestTest {
    @Test
    void testFromJsonString() {
        String jsonString = """
                {
                    "ip": "10.1.1.1",
                    "port": "9091",
                    "numberOfSeconds": "5"
                }
                """;
        StartRecordingRequest request = StartRecordingRequest.fromJsonString(jsonString);
        assertNotNull(request);
    }

}