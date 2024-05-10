package oracle.jms.jfr.collector;

import jdk.management.jfr.FlightRecorderMXBean;

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
import java.util.logging.Logger;

public class CollectorTask implements Runnable {
    private final String outputFile;
    private final StartRecordingRequest request;
    private static final Logger LOGGER = Logger.getLogger(Thread.class.getName());

    public CollectorTask(StartRecordingRequest request) {
        this.request = request;
        this.outputFile = request.getIp() + "-" + request.getPort() + "-" + System.currentTimeMillis() + "-" + ".jfr";
    }

    @Override
    public void run() {

        String url = "service:jmx:rmi:///jndi/rmi://" + this.request.getIp() + ":" + this.request.getPort() + "/jmxrmi";
        LOGGER.info(Thread.currentThread().getName() + " START connection to " + url);
        JMXServiceURL jmxServiceURL = null;
        try {
            jmxServiceURL = new JMXServiceURL(url);
        } catch (MalformedURLException e) {
            LOGGER.severe("jmx url invalid: " + e.getMessage());
        }
        MBeanServerConnection connection;

        if (jmxServiceURL == null) {
            LOGGER.severe("jmx url is null");
            return;
        }

        try (JMXConnector conn = JMXConnectorFactory.connect(jmxServiceURL)) {
            connection = conn.getMBeanServerConnection();
            FlightRecorderMXBean frb = JMX.newMXBeanProxy(connection, new ObjectName("jdk.management.jfr:type=FlightRecorder"), FlightRecorderMXBean.class);
            long recId = frb.newRecording();

            // TODO: add settings logic
            // frb.setRecordingSettings();

            LOGGER.info(Thread.currentThread().getName() + " START recording " + url);
            LOGGER.info(Thread.currentThread().getName() + " For " + this.request.getNumberOfSeconds() + " seconds ");
            frb.startRecording(recId);

            Thread.sleep(this.request.getNumberOfSeconds() * 1000L);

            LOGGER.info(Thread.currentThread().getName() + " STOP recording " + url);
            frb.stopRecording(recId);

            long streamId = frb.openStream(recId, null);

            FileOutputStream fw = new FileOutputStream(this.outputFile);

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
            throw new RuntimeException(e);
        }

        LOGGER.info(Thread.currentThread().getName() + " DONE collecting from " + this.request.getPort());
        LOGGER.info(Thread.currentThread().getName() + " Output file saved as " + this.outputFile);
    }
}
