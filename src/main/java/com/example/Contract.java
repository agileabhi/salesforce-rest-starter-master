package com.example;

import java.io.Serializable;

public class Contract implements Serializable{

    private Integer contractId;
    private String name;
    public Integer getContractId() {
        return contractId;
    }
    public void setContractId(Integer id) {
        this.contractId = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    @Override
    public String toString() {
        return "Contract [contractId=" + contractId + ", name=" + name + "]";
    }
    

    
    
}
