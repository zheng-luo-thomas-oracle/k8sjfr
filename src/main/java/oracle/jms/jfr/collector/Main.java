package oracle.jms.jfr.collector;

import io.javalin.Javalin;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.logging.Logger;


public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(
            String[] args) throws IOException, MalformedObjectNameException, InterruptedException {

        CollectorService collectorService = new CollectorService();

        var app = Javalin.create(/*config*/);

        app.get("/", ctx -> ctx.result(getHelpMsg()));

        app.get("/podInfo", ctx -> {
            LOGGER.info("RECEIVED podInfo");
            ctx.result(collectorService.getPodInfo());
        });

        app.get("/podIP", ctx -> {
            LOGGER.info("RECEIVED podIP");
            ctx.result(collectorService.getPodIP());
        });

        app.get("/samplePayload", ctx -> {
            LOGGER.info("RECEIVED samplePayload");
            ctx.result(collectorService.getSamplePayload());
        });

        app.get("/recording", ctx -> {
            //            LOGGER.info("RECEIVED GET recording");
            ctx.result(collectorService.listRecording());
        });

        app.post("/recording", ctx -> {
            LOGGER.info("RECEIVED POST recording");
            ctx.result(collectorService.startRecordings(ctx.body()));
        });

        app.start(4567);
    }

    private static String getHelpMsg() {
        return """
                 GET /
                 return help msg
                 ---
                 GET /podInfo
                 return detailed information of JMS enabled pods
                 ---
                 GET /podIP
                 return ip of JMS enabled pods
                 ---
                 GET /samplePayload
                 return sample json payload body
                 ---
                 GET /recording
                 return all submitted recording requests
                 ---
                 POST /recording
                 start JFR recording(s). Requires payload body
                """;
    }


}