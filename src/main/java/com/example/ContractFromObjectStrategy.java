package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ContractFromObjectStrategy implements ContractCretionStrategy {

    private static final String OBJECT_PATH = "/services/data/v39.0/query/";
    private static Logger LOG = LoggerFactory.getLogger(ContractFromObjectStrategy.class);

    @Override
    public List<Contract> createContract(JsonNode loginResult)
            throws JsonProcessingException, IOException, InterruptedException, ExecutionException, URISyntaxException {
        final String accessToken = loginResult.get("access_token").asText();
        final String instanceUrl = loginResult.get("instance_url").asText();
        final URIBuilder builder = new URIBuilder(instanceUrl);
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        builder.setPath(OBJECT_PATH).setParameter("q", "SELECT  Name FROM aumContract__c");
        final HttpGet get = new HttpGet(builder.build());
        get.setHeader("Authorization", "Bearer " + accessToken);

        final HttpResponse queryResponse = httpclient.execute(get);

        final JsonNode queryResults = mapper.readValue(queryResponse.getEntity().getContent(), JsonNode.class);
        LOG.info("incoming response {}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryResults));
        

        List<Contract> contracts = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(queryResults.toString()); // json is the JSON response string
        JsonNode recordsNode = (JsonNode) rootNode.get("records");
        List<Future<Contract>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(4); // 4 threads
        for (JsonNode recordNode : recordsNode) {
            ContractTask task = new ContractTask(recordNode);
            futures.add(executorService.submit(task));
        }
        for (Future<Contract> future : futures) {
            contracts.add(future.get()); // blocks until task completes
        }
        executorService.shutdown();
        // throw new UnsupportedOperationException("Unimplemented method
        // 'createContract'");
        return contracts;
    }

}