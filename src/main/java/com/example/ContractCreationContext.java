package com.example;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class ContractCreationContext {
    
    private ContractCretionStrategy strategy;

    public ContractCreationContext(ContractCretionStrategy strategy) {
        this.strategy = strategy;
    }

    public void setContractCreationStrategy(ContractCretionStrategy strategy){
        this.strategy = strategy;
    }

    public List<Contract> createContractFromJsonResponse(JsonNode jsonResponse) throws JsonProcessingException, IOException, InterruptedException, ExecutionException, URISyntaxException {
        return strategy.createContract(jsonResponse);
    }
    

}
