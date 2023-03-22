package com.example;

import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.JsonNode;

public class ContractTask implements Callable<Contract> {
    private final JsonNode recordNode;

    public ContractTask(JsonNode recordNode) {
        this.recordNode = recordNode;
    }

    @Override
    public Contract call() throws Exception {
        Contract contract = new Contract();
        contract.setName(recordNode.get("Name").asText());
        //contract.setContractId(recordNode.get("contractId").asInt());
        return contract;
    }
}
