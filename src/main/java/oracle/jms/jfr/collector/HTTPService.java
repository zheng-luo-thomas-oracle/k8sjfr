package oracle.jms.jfr.collector;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class HTTPService {
    private static final Logger LOGGER=Logger.getLogger(HTTPService.class.getName());

    public HTTPService() {
    }

    public String getPodsInfo() throws IOException, ApiException {
        return getPods().stream().map(pod -> {

            String podIP = pod.getStatus() != null ? pod.getStatus().getPodIP() : "";
            String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "";
            String podNamespace = pod.getMetadata() != null ? pod.getMetadata().getNamespace() : "";
            Map<String, String> podLabels = pod.getMetadata() != null ? pod.getMetadata().getLabels() : new HashMap<>();

            PodInfo podInfo = new PodInfo(podName, podNamespace, podIP, podLabels);
            return podInfo.toString();

        }).reduce("", (s1, s2) -> s1 + s2);

    }

    public String getPodsIP() throws IOException, ApiException {
        return getPods().stream().map(pod -> {
            return pod.getStatus() != null ? pod.getStatus().getPodIP() : "";
        }).reduce("", (s1, s2) -> s1 + s2 + "\n");
    }

    public String startRecordings(String requestBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<StartRecordingRequest> requests;
        try {
            requests = objectMapper.readValue(requestBody, new TypeReference<List<StartRecordingRequest>>() {});
        } catch (JacksonException e) {
            return "json parson error: " + e.getMessage();
        }
        LOGGER.info(requests.toString() + " in total <" + requests.size() + "> recordings requests received.");

        Collector.getInstance().startRecordings(requests);

        return "recording collected for " + requests.toString();
    }

    private List<V1Pod> getPods() throws ApiException, IOException {
        ApiClient client = ClientBuilder.cluster().build();
        Configuration.setDefaultApiClient((client));
        CoreV1Api api = new CoreV1Api();
        V1PodList list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);

        // filter for pods with label 'jmsenabnled' == 'true'
        return list.getItems().stream().filter(pod -> {
            V1ObjectMeta metadata = pod.getMetadata();
            if (metadata == null) {
                return false;
            }
            return metadata.getLabels() != null && "true".equals(metadata.getLabels().get("jmsenabled"));
        }).toList();
    }

    static class PodInfo {
        private final String name;
        private final String namespace;
        private final String podIP;
        private final Map<String, String> labels;

        public PodInfo(String name, String namespace, String podIP, Map<String, String> labels) {
            this.name = name;
            this.namespace = namespace;
            this.podIP = podIP;
            this.labels = labels;
        }

        @Override
        public String toString() {
            return "PodInfo: " + "name='" + name + '\'' + ", namespace='" + namespace + '\'' + ", podIP='" + podIP + '\'' + ", labels=" + labels + "\n";
        }
    }
}
