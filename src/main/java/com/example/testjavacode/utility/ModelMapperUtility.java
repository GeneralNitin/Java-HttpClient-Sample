package com.example.testjavacode.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ModelMapperUtility {

    private static final ModelMapper modelMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setPropertyCondition(Conditions.isNotNull())
                .setMatchingStrategy(MatchingStrategies.STRICT);
    }

    public static <D, T> D standardModelMapper(final T subject, Class<D> tClass) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STANDARD);
        return modelMapper.map(subject, tClass);
    }

    public static <D, T> List<D> standardModelsMappers(final Collection<T> entityList, Class<D> outCLass) {
        return entityList.stream()
                .map(entity -> standardModelMapper(entity, outCLass))
                .collect(Collectors.toList());
    }

    public static <S, D> D standardModelMapper(final S source, D destination) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STANDARD);
        modelMapper.map(source, destination);
        return destination;
    }

    public static <D, T> List<D> nonNullModelsMappers(final Collection<T> entityList, Class<D> outCLass) {
        return entityList.stream()
                .map(entity -> looseModelMapper(entity, outCLass))
                .collect(Collectors.toList());
    }

    public static <S, D> D nonNullModelMapper(final S source, D destination) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        modelMapper.map(source, destination);
        return destination;
    }

    public static <D, T> D nonNullStandardModelMapper(final T subject, Class<D> tClass) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STANDARD);
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        return modelMapper.map(subject, tClass);
    }

    public static <D, T> D looseModelMapper(final T subject, Class<D> tClass) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return modelMapper.map(subject, tClass);
    }

    public static <D, T> List<D> looseModelsMappers(final Collection<T> entityList, Class<D> outCLass) {
        return entityList.stream()
                .map(entity -> looseModelMapper(entity, outCLass))
                .collect(Collectors.toList());
    }

    public static <S, D> D looseModelMapper(final S source, D destination) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        modelMapper.map(source, destination);
        return destination;
    }

    public static <D, T> D mapModel(final T subject, Class<D> tClass) {
        return modelMapper.map(subject, tClass);
    }

    public static <D, T> List<D> mapModels(final Collection<T> entityList, Class<D> outCLass) {
        return entityList.stream()
                .map(entity -> mapModel(entity, outCLass))
                .collect(Collectors.toList());
    }

    public static <S, D> D mapModel(final S source, D destination) {
        modelMapper.map(source, destination);
        return destination;
    }

    public static JSONArray mapHttpResponseToJSONArray(HttpResponse httpResponse) throws IOException, ParseException {
        String responseBody = null;
        HttpEntity responseEntity = httpResponse.getEntity();
        if (responseEntity != null) {
            responseBody = EntityUtils.toString(responseEntity);
        }
        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse(responseBody);
    }

    public static JSONArray mapStringResponseToJSONArray(String responseBody) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONArray) parser.parse(responseBody);
    }

    public static JSONObject mapHttpResponseToJSONObject(HttpResponse httpResponse) throws IOException, ParseException {
        String responseBody = null;
        HttpEntity responseEntity = httpResponse.getEntity();
        if (responseEntity != null) {
            responseBody = EntityUtils.toString(responseEntity);
        }
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(responseBody);
    }

    public static JSONObject mapStringResponseToJSONObject(String responseBody) throws ParseException {
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(responseBody);
    }

    public static StringEntity mapObjectToJsonBody(Object obj) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(obj);
        return new StringEntity(json, StandardCharsets.UTF_8);
    }

    public static JSONObject mapObjectToJsonObject(Object object) throws ParseException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        return (JSONObject) parser.parse(mapper.writeValueAsString(object));
    }
}
