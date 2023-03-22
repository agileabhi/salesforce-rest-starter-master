package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class SalesforceRestStarter {

    private static final String TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";
    private static final Logger LOG = LoggerFactory.getLogger(SalesforceRestStarter.class);

    public static void main(String[] args) throws Exception {

        String username;
        String password;
        String consumerKey;
        String consumerSecret;
        if (args.length == 5) {
            username = args[0];
            password = args[1];
            consumerKey = args[2];
            consumerSecret = args[3];
        } else if (System.console() != null) {
            // System.out.print("Salesforce Username: ");
            // username = System.console().readLine();
            username = "";
            // System.out.print("Salesforce Password: ");
            // password = new String(System.console().readLine());
            password = "";
            // System.out.print("Salesforce Consumer Key: ");
            // consumerKey = System.console().readLine();
            consumerKey = "";
            // System.out.print("Salesforce Consumer Secret: ");
            // consumerSecret = new String(System.console().readLine());
            consumerSecret = "";
        } else {
            throw new Exception("You need to specify username, password, consumer key, and consumer secret");
        }

        try {
            // login

            final JsonNode loginResult = login(username, password, consumerKey, consumerSecret);

            List<Contract> contracts = null;
            ContractFromObjectStrategy contractFromObjectStrategy = new ContractFromObjectStrategy();
            ContractCreationContext context = new ContractCreationContext(contractFromObjectStrategy);
            contracts = context.createContractFromJsonResponse(loginResult);
            LOG.info("Contracts created {}", contracts);

            ContractFromReportStrategy contractFromReportStrategy = new ContractFromReportStrategy();
            context.setContractCreationStrategy(contractFromReportStrategy);
            contracts = context.createContractFromJsonResponse(loginResult);
            LOG.info("Contracts created {}", contracts);

            // post a new contract into sobject and use this inserted record to generate
            // report
            createNewRecord(loginResult);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createNewRecord(JsonNode loginResult) throws ParseException, IOException {
        HttpPost httpPost = new HttpPost(
                "https://wissentechnology-dev-ed.develop.my.salesforce.com/services/data/v39.0/sobjects/aumcontract__c");

        // Set request headers
        httpPost.addHeader("Authorization", "Bearer " + loginResult.get("access_token").asText());
        httpPost.addHeader("Content-Type", "application/json");

        // Set request body
        String requestBody = "{\"name\": \"POC7\", \"contractid__c\": \"7\"}";
        httpPost.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        // Execute HTTP request
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(httpPost);

        // Process response
        HttpEntity entity = response.getEntity();
        String responseString = null;
        if (entity != null) {
            responseString = EntityUtils.toString(entity);
            System.out.println("Response: " + responseString);
        }
   

        HttpGet httpGet = new HttpGet(
                "https://wissentechnology-dev-ed.develop.my.salesforce.com/services/data/v39.0/analytics/reports/00O2w00000D59dwEAB");

        // Set request headers
        httpGet.addHeader("Authorization", "Bearer " + loginResult.get("access_token").asText());
        httpGet.addHeader("Content-Type", "application/json");


        response = httpClient.execute(httpGet);
        // LOG.info(EntityUtils.toString(response.getEntity()));

        // parse the json and update the filter,name and condition fields
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        final JsonNode queryResults = mapper.readValue(response.getEntity().getContent(), JsonNode.class);

        JsonNode rootNode = mapper.readTree(queryResults.toString()); // json is the JSON response string
        // JsonNode recordsNode = (JsonNode) rootNode.get("reportMetaData");
        ArrayNode filtersNode = (ArrayNode) rootNode.at("/reportMetadata/reportFilters");
        ObjectNode newFilter = mapper.createObjectNode();
        newFilter.put("column", "AUMContract__c.Name");
        newFilter.put("operator", "equals");
        newFilter.put("value", "POC5");

        // Add the new filter object to the filters array
        filtersNode.add(newFilter);

        ((ObjectNode) rootNode.get("reportMetadata")).put("name", "Fee for POC6");
        ((ObjectNode) rootNode.get("reportMetadata")).put("reportBooleanFilter", "1");
        ((ObjectNode) rootNode.get("reportMetadata")).put("reportFormat", "SUMMARY");

        JsonNode recordsNode = (JsonNode) rootNode.at("/reportMetadata");
        // Serialize the updated JSON back to a string
        //String updatedJsonString = mapper.writeValueAsString(recordsNode);

        httpPost = new HttpPost(
                "https://wissentechnology-dev-ed.develop.my.salesforce.com/services/data/v39.0/analytics/reports/");

        // Set request headers
        httpPost.addHeader("Authorization", "Bearer " + loginResult.get("access_token").asText());
        httpPost.addHeader("Content-Type", "application/json");

        // Set request body
        // String requestBody = "{\"name\": \"POC7\", \"contractid__c\": \"7\"}";

        ObjectNode reportNode = mapper.createObjectNode();
        reportNode.set("reportMetadata", recordsNode);
        //reportNode.put("reportMetadata", recordsNode);
        String newJson = mapper.writeValueAsString(reportNode);
        httpPost.setEntity(new StringEntity(newJson, ContentType.APPLICATION_JSON));

        LOG.info(newJson);

        // Execute HTTP request
        response = httpClient.execute(httpPost);

        // Process response
        entity = response.getEntity();
        responseString = null;
        if (entity != null) {
            responseString = EntityUtils.toString(entity);
            System.out.println("Response: " + responseString);
        }

    }

    private static JsonNode login(String username, String password, String consumerKey, String consumerSecret)
            throws ClientProtocolException, IOException {
        final CloseableHttpClient httpclient = HttpClients.createDefault();
        final List<NameValuePair> loginParams = new ArrayList<NameValuePair>();
        loginParams.add(new BasicNameValuePair("client_id", consumerKey));
        loginParams.add(new BasicNameValuePair("client_secret", consumerSecret));
        loginParams.add(new BasicNameValuePair("grant_type", "password"));
        loginParams.add(new BasicNameValuePair("username", username));
        loginParams.add(new BasicNameValuePair("password", password));

        final HttpPost post = new HttpPost(TOKEN_URL);
        post.setEntity(new UrlEncodedFormEntity(loginParams));

        final HttpResponse loginResponse = httpclient.execute(post);

        // parse
        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        final JsonNode loginResult = mapper.readValue(loginResponse.getEntity().getContent(), JsonNode.class);
        return loginResult;
    }
}
