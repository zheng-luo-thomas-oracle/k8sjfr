package oracle.jms.jfr.collector;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.management.MalformedObjectNameException;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import static spark.Spark.get;
import static spark.Spark.post;

public class Main {
    private static final Logger LOGGER=Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException, MalformedObjectNameException, InterruptedException {
        HTTPService httpService = new HTTPService();

        get("/hello", (req, res) -> "Hello World");

        get("/getPodsInfo", (req, res) -> {
            LOGGER.info("getPodsInfo");
            return httpService.getPodsInfo();
        });

        get("/getPodsIP", (req, res) -> {
            LOGGER.info("getPodsIP");
            return httpService.getPodsIP();
        });

        post("/startRecording", (req, res) -> {
            LOGGER.info("startRecording");
            return httpService.startRecordings(req.body());
        });


    }



}