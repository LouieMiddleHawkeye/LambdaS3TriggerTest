package helloworld;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App implements
        RequestHandler<S3Event, String> {
    private final String JSON_TYPE = "json";
    private final String SEGMENT_TYPE = "segment";
    private final String JSON_MIME = (String) "application/json";
    private final String TEXT_MIME = (String) "text/plain";

    public String handleRequest(S3Event s3event, Context context) {
        S3EventNotificationRecord record = s3event.getRecords().get(0);

        String srcBucket = record.getS3().getBucket().getName();
        // Object key may have spaces or unicode non-ASCII characters.
        String srcKey = record.getS3().getObject().getUrlDecodedKey();
        String versionId = record.getS3().getObject().getVersionId();

        String dstBucket = srcBucket + "-resized";
        String dstKey = "resized-" + srcKey;

        // Sanity check: validate that source and destination are different
        // buckets.
        if (srcBucket.equals(dstBucket)) {
            System.out
                    .println("Destination bucket must not match source bucket.");
            return "";
        }

        if (record.getEventName().equals("ObjectCreated:Put")) {
            return handleCreateFile(srcKey, srcBucket, dstKey, dstBucket);
        }
//        if (record.getEventName().equals("ObjectRemoved:DeleteMarkerCreated")) {
//            return handleDeleteFile(srcKey, srcBucket, dstKey, dstBucket, versionId);
//        }

        System.out.println("Error triggering on event " + record.getEventName());

        return "";
    }

    private String handleCreateFile(String srcKey, String srcBucket, String dstKey, String dstBucket) {
        try {
            // Infer the file type.
            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(srcKey);
            if (!matcher.matches()) {
                System.out.println("Unable to infer type for key "
                        + srcKey);
                return "";
            }
            String type = matcher.group(1);
            if (!JSON_TYPE.equals(type)) {
                System.out.println("Skipping non-json " + srcKey);
                return "";
            }

            // Download the json from S3 into a stream
            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(
                    srcBucket, srcKey));
            InputStream objectData = s3Object.getObjectContent();

            // Read the source json
            String srcJson = IOUtils.toString(objectData);
            DocumentContext srcDocument = JsonPath.parse(srcJson, Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));

            String year = srcDocument.read("$.details.venue.name", String.class);

            InputStream is = new ByteArrayInputStream(year.getBytes(StandardCharsets.UTF_8));
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentLength(year.getBytes(StandardCharsets.UTF_8).length);
            meta.setContentType(TEXT_MIME);

            // Uploading to S3 destination bucket
            System.out.println("Writing to: " + dstBucket + "/" + dstKey);
            try {
                s3Client.putObject(dstBucket, dstKey, is, meta);
            } catch (AmazonServiceException e) {
                System.err.println(e.getErrorMessage());
                System.exit(1);
            }
            System.out.println("Successfully resized " + srcBucket + "/"
                    + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
            return "Ok";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    private String handleDeleteFile(String srcKey, String srcBucket, String dstKey, String dstBucket, String versionId) {
//        try {
//            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
//
//            // Infer the file type.
//            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(srcKey);
//            if (!matcher.matches()) {
//                System.out.println("Unable to infer type for key "
//                        + srcKey);
//                return "";
//            }
//            String type = matcher.group(1);
//            if (!SEGMENT_TYPE.equals(type)) {
//                System.out.println("Skipping non-segment " + srcKey);
//                return "";
//            }
//
//            // Download the json from S3 into a stream
//            S3Object s3Object = s3Client.getObject(new GetObjectRequest(
//                    srcBucket, srcKey));
//            InputStream objectData = s3Object.getObjectContent();
//
//            // Read the source json
//            String srcJson = IOUtils.toString(objectData);
//            DocumentContext srcDocument = JsonPath.parse(srcJson, Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
//
//            String year = srcDocument.read("$.details.venue.name", String.class);
//
//            InputStream is = new ByteArrayInputStream(year.getBytes(StandardCharsets.UTF_8));
//            ObjectMetadata meta = new ObjectMetadata();
//            meta.setContentLength(year.getBytes(StandardCharsets.UTF_8).length);
//            meta.setContentType(TEXT_MIME);
//
//            // Uploading to S3 destination bucket
//            System.out.println("Writing to: " + dstBucket + "/" + dstKey);
//            try {
//                s3Client.putObject(dstBucket, dstKey, is, meta);
//            } catch (AmazonServiceException e) {
//                System.err.println(e.getErrorMessage());
//                System.exit(1);
//            }
//            System.out.println("Successfully resized " + srcBucket + "/"
//                    + srcKey + " and uploaded to " + dstBucket + "/" + dstKey);
//            return "Ok";
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}

