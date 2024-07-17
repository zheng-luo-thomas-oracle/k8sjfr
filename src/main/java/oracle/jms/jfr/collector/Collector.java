package oracle.jms.jfr.collector;

import oracle.jms.jfr.collector.request.JFRRecordingRequest;
import oracle.jms.jfr.collector.request.RequestStatusEnum;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Collector {
    private static final Logger LOGGER = Logger.getLogger(Collector.class.getName());
    private static final String THREADS_ENV_VARIABLE = "JFR_COLLECTOR_THREADS";
    private static final String DEFAULT_THREADS = "5";
    private static Collector collector;
    private final List<JFRRecordingRequest> submittedRequests =
            new CopyOnWriteArrayList<JFRRecordingRequest>(); // thread safe
    private int threads;
    private ExecutorService executorService;

    private Collector() {

    }

    public static Collector getInstance() {
        if (collector == null) {
            collector = new Collector();
            collector.threads = Integer.parseInt(
                    System.getenv().getOrDefault(THREADS_ENV_VARIABLE, DEFAULT_THREADS));
            LOGGER.info("using thread pool of " + collector.threads + " threads");
            collector.executorService = Executors.newFixedThreadPool(collector.threads);
        }
        return collector;
    }

    public List<JFRRecordingRequest> getSubmittedRequests() {
        return submittedRequests;
    }

    public void addSubmittedRequest(JFRRecordingRequest newRequest) {
        submittedRequests.add(newRequest);
    }

    public void startRecordings(List<JFRRecordingRequest> requests) {
        for (JFRRecordingRequest request : requests) {
            getInstance().addSubmittedRequest(request);
        }
        createExecutor();
        for (JFRRecordingRequest request : requests) {
            Runnable collectorTask = new CollectorTask(request);
            getInstance().executorService.submit(collectorTask);
        }
        shutdownExecutor();
        //        try (ExecutorService executorService = Executors.newFixedThreadPool(
        //                getInstance().threads)) {
        //            for (JFRRecordingRequest request : requests) {
        //                Runnable collectorTask = new CollectorTask(request);
        //                executorService.submit(collectorTask);
        //            }
        //            executorService.shutdown();
        //        }
    }

    private void createExecutor() {
        if (getInstance().executorService.isShutdown()) {
            getInstance().executorService = Executors.newFixedThreadPool(getInstance().threads);
        }
    }

    private void shutdownExecutor() {
        for (JFRRecordingRequest request : getInstance().submittedRequests) {
            if (!request.getStatus().equals(RequestStatusEnum.DONE)) {
                return;
            }
        }
        getInstance().executorService.shutdown();
    }


}