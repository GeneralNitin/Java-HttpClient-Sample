package com.example.testjavacode.httpclient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClientTest {

    public static void main(String[] args) {
        final APIResponse apiResponse = HttpClientUtil.getAPIResponse(
                "",
                "",
                HttpClientUtil.getHttpGetRequest());

        log.info(apiResponse.getMessage());
    }
}
