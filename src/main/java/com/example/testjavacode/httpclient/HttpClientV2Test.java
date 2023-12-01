package com.example.testjavacode.httpclient;

import com.example.testjavacode.utility.AuthenticationType;

public class HttpClientV2Test {

    public static void main(String[] args) throws InternalServerException {
        HttpClientUtil httpClientUtil = new HttpClientUtil();

        httpClientUtil.setClientId("");
        httpClientUtil.setClientSecret("");
        httpClientUtil.setUsername("");
        httpClientUtil.setPassword("");

        final APIResponse slmStage = httpClientUtil.getAPIResponse(
                "slmStage",
                "",
                "",
                HttpClientUtil.getHttpGetRequest(),
                "",
                AuthenticationType.PASSWORD_CREDENTIALS);

        System.out.println(slmStage.getMessage());
    }
}
