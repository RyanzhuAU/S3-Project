package com.xujinpeng.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.atmosphere.config.service.Delete;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.xujinpeng.Constants;
import com.xujinpeng.kinesis.AmazonKinesisRecordProducer;
import com.xujinpeng.kinesis.FetchDataFromKinesis;

@Path("/kinesis")
@Produces(MediaType.APPLICATION_JSON)
public class KinesisResource {
	private static AmazonS3 s3client;
	private boolean runningStatus;
	
	private static String bucketName     = "upload-project";
   	private static String keyName        = "testDate";
   	private static String uploadFileName = "testdate.txt";
	
	AmazonKinesisClient kinesis;

   	private BroadcasterFactory broadcasterFactory;
	
    public KinesisResource(BroadcasterFactory broadcasterFactory) {
    	this.broadcasterFactory = broadcasterFactory;
    	
    	AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider("default").getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/xujinpeng1/.aws/credentials), and is in valid format.",
                    e);
        }
        kinesis = new AmazonKinesisClient(credentials);
    }

    @GET
    public void kinesisControl(@QueryParam("status") boolean status) {
    	runningStatus = status;
    	AmazonKinesisRecordProducer producer = new AmazonKinesisRecordProducer(kinesis);
    	//        int i = 0;
		try {
//	        producer.init();
//    	        while(i < 2) {
	        while(runningStatus) {
	        	producer.produceData(50);
	        	FetchDataFromKinesis consumer = new FetchDataFromKinesis(kinesis);
	        	consumer.fetchData();
	        	
	        	Thread.sleep(1000);
	        	uploadObject();
	        	
	        	Thread.sleep(1000);
	        	getObject();
	        	
//    	        	i++;
	        }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
   	private void uploadObject() throws IOException {
   		s3client = new AmazonS3Client(new ProfileCredentialsProvider());
   		try {
   			System.out.println("Uploading a new object to S3 from a file\n");
   			File file = new File(uploadFileName);
   			s3client.putObject(new PutObjectRequest(
           		                 bucketName, keyName, file));

   		} catch (AmazonServiceException ase) {
   			System.out.println("Caught an AmazonServiceException, which " +
   					"means your request made it " +
   					"to Amazon S3, but was rejected with an error response" +
   					" for some reason.");
   			System.out.println("Error Message:    " + ase.getMessage());
   			System.out.println("HTTP Status Code: " + ase.getStatusCode());
   			System.out.println("AWS Error Code:   " + ase.getErrorCode());
   			System.out.println("Error Type:       " + ase.getErrorType());
   			System.out.println("Request ID:       " + ase.getRequestId());
   		} catch (AmazonClientException ace) {
   			System.out.println("Caught an AmazonClientException, which " +
   					"means the client encountered " +
   					"an internal error while trying to " +
   					"communicate with S3, " +
                   "such as not being able to access the network.");
   			System.out.println("Error Message: " + ace.getMessage());
   		}
   	}
   
   	private void getObject() {
   		S3Object object = s3client.getObject(bucketName, keyName);
   	 	Broadcaster broadcaster = broadcasterFactory.lookup(Constants.APPLICATION_NAME);
   	 	
   	 	try {
	   	 	BufferedReader reader = new BufferedReader(new InputStreamReader(object.getObjectContent()));
	       	while (true) {
	       		String line = reader.readLine();
	       		if (line == null) {
	       			broadcaster.broadcast("Set Finish");
	       			break;
	       		}
	       		else {
	       			broadcaster.broadcast(line);
	       		}
	
	       		System.out.println("    " + line);
	       	}
	       	System.out.println();
       	} catch (IOException e) {
   	 		// TODO Auto-generated catch block
   	 		e.printStackTrace();
   	 	}
   	}	
   
   	@GET
   	@Path("/deleteStream")
   	public Response deleteStream() {
   		System.out.printf("Deleting the Amazon Kinesis stream used by the sample. Stream Name = %s.\n",
         		Constants.APPLICATION_STREAM_NAME);
         try {
             kinesis.deleteStream(Constants.APPLICATION_STREAM_NAME);
			 System.out.println("Delete Complete.");
         } catch (ResourceNotFoundException ex) {
             // The stream doesn't exist.
			 System.out.println("Error occurred during deleting.");
//			 return Response.serverError().build();
         }

		return Response.ok().build();
	}
//   	private void displayTextInputStream(InputStream input) throws IOException {
//   		// Read one text line at a time and display.
//       	BufferedReader reader = new BufferedReader(new InputStreamReader(input));
//       	while (true) {
//       		String line = reader.readLine();
//       		if (line == null) break;
//
//       		System.out.println("    " + line);
//       	}
//       	System.out.println();
//	}
}
