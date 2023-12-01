package com.example.testjavacode.utility;

import lombok.Getter;

@Getter
public enum AuthenticationType {
    NO_AUTH("no_auth"),
    CLIENT_CREDENTIALS("client_credentials"),
    PASSWORD_CREDENTIALS("password");

    private final String label;

    AuthenticationType(String label) {
        this.label = label;
    }
}
