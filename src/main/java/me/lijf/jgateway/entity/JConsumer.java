package me.lijf.jgateway.entity;

import lombok.Data;

import java.util.List;

@Data
public class JConsumer {
    private String appId;
    private String secretKey;
    private List<String> authorities;
}
