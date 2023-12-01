package com.example.testjavacode.httpclientv2;

import com.example.testjavacode.utility.AuthenticationType;
import com.example.testjavacode.utility.ModelMapperUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.naming.NamingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Setter
@Slf4j
public class HttpClientUtil {

    private static final Map<String, String> cachedTokens = new HashMap<>();

    private String clientId;
    private String clientSecret;
    private String username;
    private String password;

    private OAuthDataModel getURIAndToken(String tokenCacheName, String baseUrl, String relativePath, String tokenUrl, AuthenticationType authenticationType) throws NamingException, URISyntaxException, MalformedURLException, InternalServerException {

        String accessToken = "";

        if (!Objects.equals(authenticationType, AuthenticationType.NO_AUTH)) {
            if (cachedTokens.containsKey(tokenCacheName)
                    && isValidAccessToken(cachedTokens.get(tokenCacheName))) {
                accessToken = cachedTokens.get(tokenCacheName);
            } else {
                log.info("Fetching access token");
                accessToken = getAccessTokenWithAuthenticationToken(tokenUrl, authenticationType);
                cachedTokens.put(tokenCacheName, accessToken);
            }
        }

        String urlString = baseUrl + relativePath;
        URL url = new URL(urlString);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

        return new OAuthDataModel(uri, accessToken);
    }

    private String getAccessTokenWithAuthenticationToken(String tokenURL, AuthenticationType authenticationType) throws InternalServerException {
        try {
            String body = getAuthenticationBody(authenticationType);
            return getAccessToken(tokenURL, body);

        } catch (Exception e) {
            log.error("Exception while fetching token: {}", e.getMessage());
            throw new InternalServerException("Failed to get the oauth token due to: " + e.getMessage());
        }
    }

    private String getAccessToken(String tokenUrlString, String body) throws IOException, InternalServerException {
        String result = null;
        String accessToken = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(tokenUrlString);
            StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
            entity.setContentType("application/x-www-form-urlencoded");
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int status = response.getStatusLine().getStatusCode();
                log.info("HTTP Client call status: {}", status);

                if (isValidSuccessStatusCode(status)) {
                    HttpEntity responseEntity = response.getEntity();
                    if (responseEntity != null) {
                        result = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                    }
                }

                if (StringUtils.isNotBlank(result)) {
                    JsonElement jsonTree = JsonParser.parseString(result);
                    if (jsonTree != null) {
                        accessToken = jsonTree.getAsJsonObject().get("access_token").getAsString();
                    }
                }

                if (StringUtils.isBlank(accessToken)) {
                    throw new InternalServerException("Failed to get the oauth token, accessToken is null");
                }

                return accessToken;
            }
        }
    }

    public boolean isValidSuccessStatusCode(int code) {
        return (code >= 200 && code < 300);
    }

    private String getAuthenticationBody(AuthenticationType authenticationType) throws UnsupportedEncodingException, InternalServerException {
        if (AuthenticationType.CLIENT_CREDENTIALS.equals(authenticationType)) {
            String clientId = this.clientId;
            String clientSecret = this.clientSecret;
            return "grant_type=" + authenticationType.getLabel() + "&client_id=" + clientId + "&client_secret=" + clientSecret;
        } else if (AuthenticationType.PASSWORD_CREDENTIALS.equals(authenticationType)) {
            String clientId = this.clientId;
            String clientSecret = this.clientSecret;
            String username = encodeValue(this.username);
            String password = encodeValue(this.password);
            return "grant_type=" + authenticationType.getLabel() + "&username=" + username + "&password=" + password + "&client_id=" + clientId + "&client_secret=" + clientSecret;
        }

        throw new InternalServerException("Authentication type not supported");
    }

    private static boolean isValidAccessToken(String accessToken) {
        if (accessToken == null) {
            log.info("Access Token null, should fetch token first");
            return false;
        }

        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length == 2 && accessToken.endsWith(".")) {
                parts = new String[]{parts[0], parts[1], ""};
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            final JsonElement jsonElement = JsonParser.parseString(payloadJson);
            final Date tokenExpiryDate = new Date(jsonElement.getAsJsonObject().get("exp").getAsLong() * 1000);

            return tokenExpiryDate.after(new Date());
        } catch (Exception ex) {
            log.error("Exception occurred while validating existing token, fetch new token. {}", ex.getMessage());
            return false;
        }
    }

    private String encodeValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public APIResponse getAPIResponse(String tokenCacheName, String baseUrl, String relativePath, HttpRequestBase httpRequest, String tokenUrl, AuthenticationType authenticationType) throws InternalServerException {
        try {
            OAuthDataModel oAuthDataModel = getURIAndToken(tokenCacheName, baseUrl, relativePath, tokenUrl, authenticationType);

            log.info("Calling OAuth URL: {}", oAuthDataModel.getUri().toURL());
            httpRequest.setURI(oAuthDataModel.getUri());
            httpRequest.setHeader("Authorization", "Bearer " + oAuthDataModel.getAccessToken());
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                    int status = response.getStatusLine().getStatusCode();
                    APIResponse apiResponse = APIResponse.builder().statusCode(status).build();
                    log.info("HTTP Client call status: {}", status);
                    if (status >= 200 && status < 300) {
                        HttpEntity responseEntity = response.getEntity();
                        if (responseEntity == null) {
                            apiResponse.setMessage("");
                            log.error("Http Response is EMPTY");
                        } else {
                            log.debug("Response present");
                            apiResponse.setMessage(EntityUtils.toString(responseEntity));
                        }
                        return apiResponse;
                    } else {
                        throw new HttpException("HTTP Client Call failed with the response: " + EntityUtils.toString(response.getEntity()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception in getAPIResponse: {}", e.getMessage());
            throw new InternalServerException("Exception in getAPIResponse, due to " + e.getMessage());
        }
    }

    public static HttpPost getHttpPostWithRequestBody(Object object) throws JsonProcessingException {
        HttpPost httpPost = getHttpPostRequest();
        httpPost.setEntity(ModelMapperUtility.mapObjectToJsonBody(object));
        return httpPost;
    }

    public static HttpPost getHttpPostRequest() {
        HttpPost httpPost = new HttpPost();
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Accept", "application/json");
        return httpPost;
    }

    public static HttpDelete getHttpDeleteRequest() {
        HttpDelete httpDelete = new HttpDelete();
        httpDelete.setHeader("Accept", "application/json");
        return httpDelete;
    }

    public static HttpPut getHttpPutRequest(Object object) throws JsonProcessingException {
        HttpPut httpPut = new HttpPut();
        httpPut.setHeader("Content-Type", "application/json");
        httpPut.setHeader("Accept", "application/json");
        httpPut.setEntity(ModelMapperUtility.mapObjectToJsonBody(object));
        return httpPut;
    }

    public static HttpGet getHttpGetRequest() {
        HttpGet httpGet = new HttpGet();
        httpGet.setHeader("Accept", "application/json");
        return httpGet;
    }
}
    
