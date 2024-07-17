package oracle.jms.jfr.collector;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageAsync;
import com.oracle.bmc.objectstorage.ObjectStorageAsyncClient;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
import com.oracle.bmc.responses.AsyncHandler;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;


public class ObjectStorageHelper {
    private static final Logger LOGGER = Logger.getLogger(ObjectStreamException.class.getName());
    private static final String BUCKET_ENV_VARIABLE_NAME = "JFR_COLLECTOR_BUCKET";
    private static final String DEFAULT_BUCKET_NAME = "bucket-k8sjfr";
    private final AuthenticationDetailsProvider authProvider;
    private final ObjectStorageAsync client;
    private final String bucketName;
    private String namespace;

    public ObjectStorageHelper() throws IOException {
        bucketName = System.getenv().getOrDefault(BUCKET_ENV_VARIABLE_NAME, DEFAULT_BUCKET_NAME);
        LOGGER.info("Using bucket: " + bucketName);

        // parse config from default config location
        ConfigFileReader.ConfigFile configFile = null;
        try {
            configFile = ConfigFileReader.parseDefault();
        } catch (IOException e) {
            LOGGER.severe("failed to parse config file from root/.oci/config. Error: " + e);
            throw e;
        }

        this.authProvider =
                new ConfigFileAuthenticationDetailsProvider(Objects.requireNonNull(configFile));
        this.client =
                ObjectStorageAsyncClient.builder().region(Region.US_ASHBURN_1).build(authProvider);
        this.setNamespace();
    }

    private void setNamespace() {
        ResponseHandler<GetNamespaceRequest, GetNamespaceResponse> namespaceHandler =
                new ResponseHandler<>();
        client.getNamespace(GetNamespaceRequest.builder().build(), namespaceHandler);
        GetNamespaceResponse namespaceResponse = null;
        try {
            namespaceResponse = namespaceHandler.waitForCompletion();
        } catch (Exception e) {
            LOGGER.severe("failed to get namespace of tenancy. Error: " + e);
        }
        this.namespace = Objects.requireNonNull(namespaceResponse).getValue();
        LOGGER.info("Using namespace: " + this.namespace);
    }

    public List<ObjectSummary> listObjects() {

        ListObjectsRequest.Builder objectsBuilder =
                ListObjectsRequest.builder().namespaceName(this.namespace)
                                  .bucketName(this.bucketName);
        ResponseHandler<ListObjectsRequest, ListObjectsResponse> listHandler =
                new ResponseHandler<>();
        client.listObjects(objectsBuilder.build(), listHandler);

        ListObjectsResponse listObjectsResponse = null;
        try {
            listObjectsResponse = listHandler.waitForCompletion();
        } catch (Exception e) {
            LOGGER.severe(
                    "failed to list objects.\nnamespace: " + this.namespace + "\nbucket: " + this.bucketName + ". Error: " + e);
        }

        return listObjectsResponse == null ? null : listObjectsResponse.getListObjects()
                                                                       .getObjects();
    }

    public void uploadObject(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            LOGGER.severe("invalid filepath: " + filePath + " Error: " + e.getMessage());
        }
        PutObjectRequest.Builder requestBuilder =
                PutObjectRequest.builder().bucketName(bucketName).namespaceName(this.namespace)
                                .objectName(new File(filePath).getName())
                                .putObjectBody(inputStream);
        ResponseHandler<PutObjectRequest, PutObjectResponse> putHandler = new ResponseHandler<>();
        client.putObject(requestBuilder.build(), putHandler);
        PutObjectResponse putObjectResponse = null;
        try {
            putObjectResponse = putHandler.waitForCompletion();
        } catch (Exception e) {
            LOGGER.severe("put objects failed: " + filePath + " Error: " + e.getMessage());
        }
        LOGGER.info("Uploaded: " + Objects.requireNonNull(putObjectResponse).getETag());
    }

    private static class ResponseHandler<IN, OUT> implements AsyncHandler<IN, OUT> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private OUT item;
        private Throwable failed = null;

        private OUT waitForCompletion() throws Exception {
            latch.await();
            if (failed != null) {
                if (failed instanceof Exception) {
                    throw (Exception) failed;
                }
                throw (Error) failed;
            }
            return item;
        }

        @Override
        public void onSuccess(IN request, OUT response) {
            item = response;
            latch.countDown();
        }

        @Override
        public void onError(IN request, Throwable error) {
            failed = error;
            latch.countDown();
        }
    }
}
