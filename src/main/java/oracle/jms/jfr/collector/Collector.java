package oracle.jms.jfr.collector;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Collector {
    private static Collector collector;

    private Collector() {

    }

    public static Collector getInstance() {
        if (collector == null) {
            collector = new Collector();
        }
        return collector;
    }

    public void startRecordings(List<StartRecordingRequest> requests) {
        try (ExecutorService executorService = Executors.newFixedThreadPool(5);) {
            for (StartRecordingRequest request : requests) {
                Runnable collectorTask = new CollectorTask(request);
                executorService.submit(collectorTask);
            }
            executorService.shutdown();
        }
    }



}