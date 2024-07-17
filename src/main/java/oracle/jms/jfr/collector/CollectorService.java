package oracle.jms.jfr.collector;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import oracle.jms.jfr.collector.request.JFRRecordingRequest;
import oracle.jms.jfr.collector.request.JFRRecordingRequestPayload;
import oracle.jms.jfr.collector.request.RequestStatusEnum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class CollectorService {
    private static final Logger LOGGER = Logger.getLogger(CollectorService.class.getName());
    private final String ENV_JMX_REMOTE_PORT = "JMX_REMOTE_PORT";
    private final String DEFAULT_JMX_REMOTE_PORT = "9091";
    private final int DEFAULT_RECORDING_SECONDS = 20;

    public CollectorService() {
    }

    private static List<JFRRecordingRequestPayload> parsePayloadJson(
            String requestBody) throws JsonProcessingException {
        List<JFRRecordingRequestPayload> payloads;
        ObjectMapper objectMapper = new ObjectMapper();
        payloads = objectMapper.readValue(requestBody,
                new TypeReference<List<JFRRecordingRequestPayload>>() {
                });
        LOGGER.info(
                payloads.toString() + " in total <" + payloads.size() + "> " + "recordings " +
                        "requests received.");
        return payloads;
    }

    public String getPodInfo() throws IOException, ApiException {
        List<PodInfo> podInfoList = getPods().stream().map(pod -> {
            String podIP = pod.getStatus() != null ? pod.getStatus().getPodIP() : "";
            String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "";
            String podNamespace = pod.getMetadata() != null ? pod.getMetadata().getNamespace() : "";
            Map<String, String> podLabels =
                    pod.getMetadata() != null ? pod.getMetadata().getLabels() : new HashMap<>();
            List<V1Container> containers = Objects.requireNonNull(pod.getSpec()).getContainers();

            return new PodInfo(podName, podNamespace, podIP, podLabels, containers);
        }).toList();
        return new ObjectMapper().writeValueAsString(podInfoList);
    }

    public String getPodIP() throws IOException, ApiException {
        return getPods().stream().map(pod -> {
            return pod.getStatus() != null ? pod.getStatus().getPodIP() : "";
        }).reduce("", (s1, s2) -> s1 + s2 + "\n");
    }

    public String startRecordings(String requestBody) {
        List<JFRRecordingRequestPayload> payloads;
        try {
            payloads = parsePayloadJson(requestBody);
        } catch (JacksonException e) {
            return "json parse error: " + e.getMessage();
        }

        List<JFRRecordingRequest> requests = payloads.stream().map(payload -> {
            JFRRecordingRequest request = new JFRRecordingRequest(payload);
            request.generateOutputFileName();
            request.setRequestStatus(RequestStatusEnum.PENDING);
            return request;
        }).toList();

        CompletableFuture<Void> recordTask = CompletableFuture.runAsync(() -> {
            Collector.getInstance().startRecordings(requests);
        });

        return requests.size() + " recording(s) started.\n";
    }

    private List<V1Pod> getPods() throws ApiException, IOException {
        ApiClient client = ClientBuilder.cluster().build();
        Configuration.setDefaultApiClient((client));
        CoreV1Api api = new CoreV1Api();
        V1PodList list =
                api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null,
                        null);

        return list.getItems().stream().filter(pod -> {
            V1ObjectMeta metadata = pod.getMetadata();
            if (metadata == null || metadata.getLabels() == null) {
                return false;
            }
            return "true".equals(metadata.getLabels().get("jmsenabled"));
        }).toList();
    }

    public String listRecording() {
        return Collector.getInstance().getSubmittedRequests().stream()
                        .map(JFRRecordingRequest::toString)
                        .reduce(Collector.getInstance().getSubmittedRequests()
                                         .size() + " request(s) submitted.",
                                (str1, str2) -> str1 + "\n" + str2);
    }

    public String getSamplePayload() throws IOException, ApiException {
        List<JFRRecordingRequestPayload> requests = getPods().stream().map(pod -> {
            List<V1Container> containers = Objects.requireNonNull(pod.getSpec()).getContainers();
            List<JFRRecordingRequestPayload> payloads = new ArrayList<>();
            if (containers.size() < 2) {
                JFRRecordingRequestPayload payload = new JFRRecordingRequestPayload();
                payload.setIp(Objects.requireNonNull(pod.getStatus()).getPodIP());
                payload.setPort(DEFAULT_JMX_REMOTE_PORT);
                payload.setNumberOfSeconds(DEFAULT_RECORDING_SECONDS);
                payloads.add(payload);
            } else {
                for (V1Container container : containers) {
                    JFRRecordingRequestPayload payload = new JFRRecordingRequestPayload();
                    payload.setIp(Objects.requireNonNull(pod.getStatus()).getPodIP());
                    V1EnvVar v1EnvVar = container.getEnv().stream().filter(v -> {
                        return v.getName().equals(ENV_JMX_REMOTE_PORT);
                    }).findAny().get();
                    payload.setPort(v1EnvVar.getValue());
                    payload.setNumberOfSeconds(DEFAULT_RECORDING_SECONDS);
                    payloads.add(payload);
                }
            }
            return payloads;
        }).flatMap(Collection::stream).toList();

        ObjectMapper mapper = new ObjectMapper();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, requests);
        return out.toString();
    }

    private record PodInfo(@JsonProperty("name") String name,
                           @JsonProperty("namespace") String namespace,
                           @JsonProperty("podIP") String podIP,
                           @JsonProperty("labels") Map<String, String> labels,
                           @JsonProperty("containers") List<V1Container> containers) {
        private PodInfo(String name, String namespace, String podIP, Map<String, String> labels,
                        List<V1Container> containers) {
            this.name = name;
            this.namespace = namespace;
            this.podIP = podIP;
            this.labels = labels;
            this.containers = containers;
        }
    }
}
