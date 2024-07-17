package oracle.jms.jfr.collector;

import oracle.jms.jfr.collector.request.JFRRecordingRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JFRRecordingRequestTest {
    @Test
    void testFromJsonPayload() {
        String jsonString = """
                {
                    "ip": "10.1.1.1",
                    "port": "9091",
                    "numberOfSeconds": "5"
                }
                """;
        JFRRecordingRequest request = JFRRecordingRequest.fromJsonPayload(jsonString);
        assertNotNull(request);
    }

}