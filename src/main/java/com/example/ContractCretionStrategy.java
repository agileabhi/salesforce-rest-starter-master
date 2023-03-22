package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public interface ContractCretionStrategy {
    
    public List<Contract> createContract(JsonNode loginResult) throws JsonProcessingException, IOException, InterruptedException, ExecutionException, URISyntaxException;
}
