package com.aivanouski.im.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.otp")
public class OtpProperties {
    private List<String> testPhones = new ArrayList<>();
    private String testCode = "000000";

    public List<String> getTestPhones() {
        return testPhones;
    }

    public void setTestPhones(List<String> testPhones) {
        this.testPhones = testPhones;
    }

    public String getTestCode() {
        return testCode;
    }

    public void setTestCode(String testCode) {
        this.testCode = testCode;
    }

}
