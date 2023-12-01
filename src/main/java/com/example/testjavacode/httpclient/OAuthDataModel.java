package com.example.testjavacode.httpclient;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
class OAuthDataModel {
    private URI uri;
    private String accessToken;
}


