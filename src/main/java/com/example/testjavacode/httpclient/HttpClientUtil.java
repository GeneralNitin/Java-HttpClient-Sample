package com.example.testjavacode.httpclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class HttpClientUtil {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    private static OAuthDataModel getDestinationURIAndToken(String baseUrl, String relativePath) throws MalformedURLException, URISyntaxException {

        String accessToken = getAccessTokenWithClientCredentials();

        String urlString = baseUrl + relativePath;
        URL url = new URL(urlString);
        URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());

        return new OAuthDataModel(uri, accessToken);
    }

    private static String getAccessTokenWithClientCredentials() {
        String result = "";

        try {
            String tokenUrlString = "";
            String body = getOAuth2AuthorizationBody("client_credentials");
            return getAccessToken(result, tokenUrlString, body);

        } catch (Exception e) {
            logger.error("Exception while fetching token: {}", e.getMessage());
            throw new RuntimeException("Failed to get the oauth token due to: " + e.getMessage());
        }
    }

    private static String getAccessTokenWithPasswordCredentials() {
        String result = "";

        try {
            String tokenUrlString = "";
            String body = getOAuth2AuthorizationBody("password");
            return getAccessToken(result, tokenUrlString, body);

        } catch (Exception e) {
            logger.error("Exception while fetching token: {}", e.getMessage());
            throw new RuntimeException("Failed to get the oauth token due to: " + e.getMessage());
        }
    }

    private static String getAccessToken(String result, String tokenUrlString, String body) throws IOException {
        String accessToken = null;
        byte[] postData = body.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;

        URL tokenURL = new URL(tokenUrlString);

        HttpURLConnection urlConnection = (HttpURLConnection) tokenURL.openConnection(Proxy.NO_PROXY);
        urlConnection.setDoOutput(true);
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("charset", "utf-8");
        urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        urlConnection.setUseCaches(false);

        try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
            wr.write(postData);
        }

        urlConnection.connect();

        int responseCode = urlConnection.getResponseCode();
        logger.info("OAuth Token Call: Response Code {}", responseCode);

        if (responseCode >= 400) {
            try (InputStream er = urlConnection.getErrorStream()) {
                String error = IOUtils.toString(er, "UTF-8");
                if (error != null && !error.isEmpty()) {
                    logger.error("OAuthTokenFetch: Error Response: {}", error);
                }
            }
            throw new RuntimeException("Failed to get the oauth token, response status " + responseCode);
        }

        if (responseCode >= 200 && responseCode < 300) {
            try (InputStream in = urlConnection.getInputStream()) {
                result = IOUtils.toString(in, "UTF-8");
            }
        }

        if (!result.isEmpty()) {
            JsonElement jsonTree = JsonParser.parseString(result);
            if (jsonTree != null) {
                accessToken = jsonTree.getAsJsonObject().get("access_token").getAsString();
            }
        }

        if (accessToken == null) {
            throw new RuntimeException("Failed to get the oauth token, accessToken is null");
        }

        logger.info("Access token fetched successfully");
        return accessToken;
    }

    private static String getOAuth2AuthorizationBody(String grantType) throws UnsupportedEncodingException {
        String clientId = "";
        String clientSecret = "";
        if (grantType.equals("client_credentials")) {
            return "grant_type=" + grantType + "&client_id=" + clientId + "&client_secret=" + clientSecret;
        } else if (grantType.equals("password")) {
            String username = encodeValue("");
            String password = encodeValue("");
            return "grant_type=" + grantType + "&username=" + username + "&password=" + password + "&client_id=" + clientId + "&client_secret=" + clientSecret;
        }

        throw new RuntimeException("Grant Type Not Implemented");
    }

    private static boolean isValidAccessToken(String accessToken) {
//        if (accessToken == null) {
//            logger.info("Access Token null, should fetch token first");
        return false;
//        }
//
//        try {
//            String[] parts = accessToken.split("\\.");
//            if (parts.length == 2 && accessToken.endsWith(".")) {
//                parts = new String[]{parts[0], parts[1], ""};
//            }
//            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
//
//            final JsonElement jsonElement = JsonParser.parseString(payloadJson);
//            final Date tokenExpiryDate = new Date(jsonElement.getAsJsonObject().get("exp").getAsLong() * 1000);
//
//            return tokenExpiryDate.after(new Date());
//        } catch (Exception ex) {
//            logger.error("Exception occurred while validating existing token, fetch new token. {}", ex.getMessage());
//            return false;
//        }
    }

    private static String encodeValue(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
    }

//    public static APIResponse executeHttpClient(HttpRequestBase request) throws InternalServerException {
//        try {
//            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
//                try (CloseableHttpResponse response = httpClient.execute(request)) {
//                    int status = response.getStatusLine().getStatusCode();
//                    APIResponse apiResponse = APIResponse.builder().statusCode(status).build();
//                    logger.info("HTTP Client call status: {}", status);
//                    if (status >= 200 && status < 300) {
//                        HttpEntity responseEntity = response.getEntity();
//                        if (responseEntity == null) {
//                            apiResponse.setMessage("");
//                            logger.debug("Response is EMPTY");
//                        } else {
//                            logger.debug("Response present");
//                            apiResponse.setMessage(EntityUtils.toString(responseEntity));
//                        }
//                        return apiResponse;
//                    } else {
//                        throw new HttpException("HTTP Client Call failed with the response: " + EntityUtils.toString(response.getEntity()));
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("Exception in executeHttpClient: {}", e.getMessage());
//            throw new InternalServerException("Exception in executeHttpClient, due to " + e.getMessage());
//        }
//    }

    public static APIResponse getAPIResponse(String baseUrl, String relativePath, HttpRequestBase httpRequest) {
        try {
            OAuthDataModel oAuthDataModel = getDestinationURIAndToken(baseUrl, relativePath);

            logger.info("Calling OAuth URL: {}", oAuthDataModel.getUri().toURL());
            httpRequest.setURI(oAuthDataModel.getUri());
            httpRequest.setHeader("Authorization", "Bearer " + oAuthDataModel.getAccessToken());
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                    int status = response.getStatusLine().getStatusCode();
                    APIResponse apiResponse = APIResponse.builder().statusCode(status).build();
                    logger.info("HTTP Client call status: {}", status);
                    if (status >= 200 && status < 300) {
                        HttpEntity responseEntity = response.getEntity();
                        if (responseEntity == null) {
                            apiResponse.setMessage("");
                            logger.error("Http Response is EMPTY");
                        } else {
                            logger.debug("Response present");
                            apiResponse.setMessage(EntityUtils.toString(responseEntity));
                        }
                        return apiResponse;
                    } else {
                        throw new HttpException("HTTP Client Call failed with the response: " + EntityUtils.toString(response.getEntity()));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in getAPIResponse: {}", e.getMessage());
            throw new RuntimeException("Exception in getAPIResponse, due to " + e.getMessage());
        }
    }

    public static HttpPost getHttpPostWithRequestBody(Object object) throws UnsupportedEncodingException, JsonProcessingException {
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

    public static HttpPut getHttpPutRequest(Object object) throws UnsupportedEncodingException, JsonProcessingException {
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