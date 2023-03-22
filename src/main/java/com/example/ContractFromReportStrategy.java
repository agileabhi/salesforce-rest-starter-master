package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

public class ContractFromReportStrategy implements ContractCretionStrategy {

    private static final String REPORT_PATH = "/services/data/v39.0/analytics/reports/";
    private static final Logger LOG = LoggerFactory.getLogger(ContractFromReportStrategy.class);

    @Override
    public List<Contract> createContract(JsonNode loginResult)
            throws JsonProcessingException, IOException, InterruptedException, ExecutionException, URISyntaxException {

        final String accessToken = loginResult.get("access_token").asText();
        final String instanceUrl = loginResult.get("instance_url").asText();
        final URIBuilder builder = new URIBuilder(instanceUrl);
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        builder.setPath(REPORT_PATH);
        HttpGet get = new HttpGet(builder.build());
        get.setHeader("Authorization", "Bearer " + accessToken);
        final HttpResponse queryResponse = httpclient.execute(get);
        final JsonNode queryRes = mapper.readValue(queryResponse.getEntity().getContent(), JsonNode.class);
       // LOG.info("incoming response {}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryRes));
        String id = " ";
       
         for(JsonNode reportNode:queryRes){
            String name = reportNode.path("name").asText();
            if(name.contains("AUM")){
                id= reportNode.path("id").asText();
                break;
            }
         }

        LOG.info("id {}",id);
        String newPath = REPORT_PATH + id;
        builder.setPath(newPath);
        get = new HttpGet(builder.build());
        get.setHeader("Authorization", "Bearer " + accessToken);

        final HttpResponse queryResults = httpclient.execute(get);
        final JsonNode response = mapper.readValue(queryResults.getEntity().getContent(), JsonNode.class);
        List<Contract> contracts = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(response.toString());
        JsonNode rowsNode = rootNode.path("factMap").path("T!T").path("rows");

        // Iterate through each row and retrieve the label field from the dataCells
        // array
        for (JsonNode rowNode : rowsNode) {
            JsonNode dataCellsNode = rowNode.path("dataCells");
            Contract newContract = new Contract();
            for (JsonNode dataCellNode : dataCellsNode) {
                String label = dataCellNode.path("label").asText();
                newContract.setName(label);
            }

            contracts.add(newContract);
        }
        return contracts;
    }

}
