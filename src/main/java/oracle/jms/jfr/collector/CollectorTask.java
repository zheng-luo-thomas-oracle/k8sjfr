package oracle.jms.jfr.collector;

import jdk.management.jfr.FlightRecorderMXBean;
import oracle.jms.jfr.collector.request.JFRRecordingRequest;
import oracle.jms.jfr.collector.request.RequestStatusEnum;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.logging.Logger;

public class CollectorTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Thread.class.getName());
    private final JFRRecordingRequest request;

    public CollectorTask(JFRRecordingRequest request) {
        this.request = request;
    }

    @Override
    public void run() {
        collectRecording();
        if (!request.getStatus().equals(RequestStatusEnum.ERROR)) {
            uploadRecording();
            this.request.setRequestStatus(RequestStatusEnum.DONE);
        }
    }

    private void uploadRecording() {
        this.request.setRequestStatus(RequestStatusEnum.UPLOADING);
        ObjectStorageHelper helper = null;
        try {
            helper = new ObjectStorageHelper();
        } catch (IOException e) {
            this.request.setRequestStatus(RequestStatusEnum.ERROR);
            throw new RuntimeException(e);
        }
        helper.uploadObject(request.getOutputFileName());
    }

    private void collectRecording() {
        this.request.setRequestStatus(RequestStatusEnum.COLLECTING);
        String url = "service:jmx:rmi:///jndi/rmi://" + request.getPayload()
                                                               .getIp() + ":" + request.getPayload()
                                                                                       .getPort() + "/jmxrmi";
        LOGGER.info(Thread.currentThread().getName() + " START connection to " + url);
        JMXServiceURL jmxServiceURL;
        try {
            jmxServiceURL = new JMXServiceURL(url);
        } catch (MalformedURLException e) {
            LOGGER.severe("jmx url invalid: " + e.getMessage());
            this.request.setRequestStatus(RequestStatusEnum.ERROR);
            return;
        }
        MBeanServerConnection connection;

        try (JMXConnector conn = JMXConnectorFactory.connect(
                Objects.requireNonNull(jmxServiceURL))) {
            connection = conn.getMBeanServerConnection();
            FlightRecorderMXBean frb = JMX.newMXBeanProxy(connection,
                    new ObjectName("jdk.management.jfr:type=FlightRecorder"),
                    FlightRecorderMXBean.class);
            long recId = frb.newRecording();

            // TODO: add settings logic
            // frb.setRecordingSettings();

            LOGGER.info(Thread.currentThread()
                              .getName() + " START recording " + request.getOutputFileName() +
                    "\nFor " + request.getPayload()
                                                                                                                 .getNumberOfSeconds() + " seconds ");
            frb.startRecording(recId);

            Thread.sleep(request.getPayload().getNumberOfSeconds() * 1000L);

            LOGGER.info(Thread.currentThread().getName() + " STOP recording " + url);
            frb.stopRecording(recId);

            long streamId = frb.openStream(recId, null);

            FileOutputStream fw = new FileOutputStream(request.getOutputFileName());

            byte[] buff = frb.readStream(streamId);
            while (buff != null) {
                buff = frb.readStream(streamId);
                if (buff != null) {
                    //System.out.println("buff.length=" + buff.length);
                    fw.write(buff);
                }
            }
            frb.closeStream(streamId);
            fw.flush();
            fw.close();
        } catch (IOException | MalformedObjectNameException | InterruptedException e) {
            LOGGER.severe("Request: " + request + "\nerror when collection: " + e);
            this.request.setRequestStatus(RequestStatusEnum.ERROR);
            return;
        }
        LOGGER.info(Thread.currentThread().getName() + " DONE collecting " + request);
    }
}
