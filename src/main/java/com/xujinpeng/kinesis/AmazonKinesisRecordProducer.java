package com.xujinpeng.kinesis;
/*
 * Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.nio.ByteBuffer;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.ListStreamsRequest;
import com.amazonaws.services.kinesis.model.ListStreamsResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.kinesis.model.StreamDescription;
import com.xujinpeng.Constants;

public class AmazonKinesisRecordProducer {

    /*
     * Before running the code:
     *      Fill in your AWS access credentials in the provided credentials
     *      file template, and be sure to move the file to the default location
     *      (/Users/xujinpeng1/.aws/credentials) where the sample code will load the
     *      credentials from.
     *      https://console.aws.amazon.com/iam/home?#security_credential
     *
     * WARNING:
     *      To avoid accidental leakage of your credentials, DO NOT keep
     *      the credentials file in your source directory.
     */

    private AmazonKinesisClient kinesis;
    
    public AmazonKinesisRecordProducer(AmazonKinesisClient kinesis) {
    	this.kinesis = kinesis;
    }

    public void produceData(int counter) throws InterruptedException{
    	String myStreamName = Constants.APPLICATION_STREAM_NAME;
        Integer myStreamSize = 1;

        // Describe the stream and check if it exists.
        DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest().withStreamName(myStreamName);
        try {
            StreamDescription streamDescription = kinesis.describeStream(describeStreamRequest).getStreamDescription();
            System.out.printf("Stream %s has a status of %s.\n", myStreamName, streamDescription.getStreamStatus());

            if ("DELETING".equals(streamDescription.getStreamStatus())) {
                System.out.println("Stream is being deleted. This sample will now exit.");
                waitForStreamToBecomeAvailable(myStreamName);
//                System.exit(0);
            }

            // Wait for the stream to become active if it is not yet ACTIVE.
            if (!"ACTIVE".equals(streamDescription.getStreamStatus())) {
                waitForStreamToBecomeAvailable(myStreamName);
            }
        } catch (ResourceNotFoundException ex) {
            System.out.printf("Stream %s does not exist. Creating it now.\n", myStreamName);

            // Create a stream. The number of shards determines the provisioned throughput.
            CreateStreamRequest createStreamRequest = new CreateStreamRequest();
            createStreamRequest.setStreamName(myStreamName);
            createStreamRequest.setShardCount(myStreamSize);
            kinesis.createStream(createStreamRequest);
            // The stream is now being created. Wait for it to become active.
            waitForStreamToBecomeAvailable(myStreamName);
        }

        // List all of my streams.
        ListStreamsRequest listStreamsRequest = new ListStreamsRequest();
        listStreamsRequest.setLimit(10);
        ListStreamsResult listStreamsResult = kinesis.listStreams(listStreamsRequest);
        List<String> streamNames = listStreamsResult.getStreamNames();
        while (listStreamsResult.isHasMoreStreams()) {
            if (streamNames.size() > 0) {
                listStreamsRequest.setExclusiveStartStreamName(streamNames.get(streamNames.size() - 1));
            }

            listStreamsResult = kinesis.listStreams(listStreamsRequest);
            streamNames.addAll(listStreamsResult.getStreamNames());
        }
        // Print all of my streams.
        System.out.println("List of my streams: ");
        for (int i = 0; i < streamNames.size(); i++) {
            System.out.println("\t- " + streamNames.get(i));
        }

        System.out.printf("Putting records in stream : %s until this application is stopped...\n", myStreamName);
        System.out.println("Press CTRL-C to stop.");
        // Write records to the stream until this program is aborted.
        for (int i = 0; i < counter; i++) {
            long createTime = System.currentTimeMillis();
            PutRecordRequest putRecordRequest = new PutRecordRequest();
            putRecordRequest.setStreamName(myStreamName);

            Date loginDate = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1));
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");

            System.out.println(dateFormat.format(loginDate));

            String randomData = String.format("{\"username\":\"%s\",  \"loginTime\":\"%s\", \"gameDuration\":\"%s\", \"gameResult\":\"%s\", \"isBlackJack\":\"%s\"}\n",
                    getRandomUserName(), dateFormat.format(loginDate), getRandomGameDuration(), getRandomGameResult(), getRandomBlackJackResult());
            putRecordRequest.setData(ByteBuffer.wrap(randomData.getBytes()));

            putRecordRequest.setPartitionKey(String.format("partitionKey-%d", createTime));

            kinesis.putRecord(putRecordRequest);
            System.out.printf(randomData);
        }
    }

    private String getRandomUserName() {
        String[] userNames = {"Jeremy", "Jason", "Tom", "Wendy"};

        int idx = new Random().nextInt(userNames.length);
        return userNames[idx];
    }

    private String getRandomGameDuration() {
        final Random random = new Random();
        final int millisInDay = 20*60*1000;
        Time time = new Time((long)random.nextInt(millisInDay));

        DateFormat timeFormat = new SimpleDateFormat("mm:ss");

        return timeFormat.format(time);
    }

    private String getRandomGameResult() {
        boolean random = new Random().nextBoolean();
        if (random) {
            return "Win";
        }
        else {
            return "Lose";
        }
    }

    private String getRandomBlackJackResult() {
        return String.valueOf(new Random().nextBoolean());
    }
    
    private void waitForStreamToBecomeAvailable(String myStreamName) throws InterruptedException {
        System.out.printf("Waiting for %s to become ACTIVE...\n", myStreamName);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + TimeUnit.MINUTES.toMillis(10);
        while (System.currentTimeMillis() < endTime) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(20));

            try {
                DescribeStreamRequest describeStreamRequest = new DescribeStreamRequest();
                describeStreamRequest.setStreamName(myStreamName);
                // ask for no more than 10 shards at a time -- this is an optional parameter
                describeStreamRequest.setLimit(10);
                DescribeStreamResult describeStreamResponse = kinesis.describeStream(describeStreamRequest);

                String streamStatus = describeStreamResponse.getStreamDescription().getStreamStatus();
                System.out.printf("\t- current state: %s\n", streamStatus);
                if ("ACTIVE".equals(streamStatus)) {
                    return;
                }
            } catch (ResourceNotFoundException ex) {
                // ResourceNotFound means the stream doesn't exist yet,
                // so ignore this error and just keep polling.
            } catch (AmazonServiceException ase) {
                throw ase;
            }
        }
        throw new RuntimeException(String.format("Stream %s never became active", myStreamName));
    }
}
