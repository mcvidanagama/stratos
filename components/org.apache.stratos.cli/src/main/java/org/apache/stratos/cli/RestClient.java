/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.stratos.cli.exception.ExceptionMapper;
import org.apache.stratos.cli.utils.CliUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RestClient implements GenericRestClient {

    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private String baseURL;
    private String username;
    private String password;

    private final int TIME_OUT_PARAM = 6000000;

    public RestClient(String baseURL, String username, String password) {
        this.baseURL = baseURL;
        this.username = username;
        this.password = password;
    }

    public String getBaseURL() {
        return baseURL;
    }

    /**
     * Handle http post request. Return String
     *
     * @param httpClient      This should be httpClient which used to connect to rest endpoint
     * @param resourcePath    This should be REST endpoint
     * @param jsonParamString The json string which should be executed from the post request
     * @return The HttpResponse
     * @throws IOException if any errors occur when executing the request
     */
    public HttpResponse doPost(DefaultHttpClient httpClient, String resourcePath, String jsonParamString)
            throws IOException {
        HttpPost postRequest = new HttpPost(resourcePath);

        StringEntity input = new StringEntity(jsonParamString);
        input.setContentType("application/json");
        postRequest.setEntity(input);

        String userPass = username + ":" + password;
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
        postRequest.addHeader("Authorization", basicAuth);

        httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_PARAM);
        HttpConnectionParams.setSoTimeout(params, TIME_OUT_PARAM);

        HttpResponse response = httpClient.execute(postRequest);
        return response;
    }

    /**
     * Handle http get request. Return String
     *
     * @param httpClient   This should be httpClient which used to connect to rest endpoint
     * @param resourcePath This should be REST endpoint
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *                                                        if any errors occur when executing the request
     */
    public HttpResponse doGet(DefaultHttpClient httpClient, String resourcePath) throws IOException {
        HttpGet getRequest = new HttpGet(resourcePath);
        getRequest.addHeader("Content-Type", "application/json");

        String userPass = username + ":" + password;
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
        getRequest.addHeader("Authorization", basicAuth);

        httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_PARAM);
        HttpConnectionParams.setSoTimeout(params, TIME_OUT_PARAM);

        HttpResponse response = httpClient.execute(getRequest);
        return response;
    }

    public HttpResponse doDelete(DefaultHttpClient httpClient, String resourcePath) throws IOException {
        HttpDelete httpDelete = new HttpDelete(resourcePath);
        httpDelete.addHeader("Content-Type", "application/json");

        String userPass = username + ":" + password;
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
        httpDelete.addHeader("Authorization", basicAuth);

        httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_PARAM);
        HttpConnectionParams.setSoTimeout(params, TIME_OUT_PARAM);

        HttpResponse response = httpClient.execute(httpDelete);
        return response;
    }

    public HttpResponse doPut(DefaultHttpClient httpClient, String resourcePath, String jsonParamString) throws IOException {
        HttpPut httpPutRequest = new HttpPut(resourcePath);

        StringEntity input = new StringEntity(jsonParamString);
        input.setContentType("application/json");
        httpPutRequest.setEntity(input);

        String userPass = username + ":" + password;
        String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPass.getBytes("UTF-8"));
        httpPutRequest.addHeader("Authorization", basicAuth);

        httpClient = (DefaultHttpClient) WebClientWrapper.wrapClient(httpClient);

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, TIME_OUT_PARAM);
        HttpConnectionParams.setSoTimeout(params, TIME_OUT_PARAM);

        HttpResponse response = httpClient.execute(httpPutRequest);
        return response;
    }

    public void deployEntity(String serviceEndpoint, String entityBody, String entityName) {
        try {
            int responseCode = executePost(serviceEndpoint, entityBody);
            if (responseCode == 201) {
                System.out.println(String.format("Successfully deployed %s", entityName));
            }
        } catch (Exception e) {
            String message = String.format("Error in deploying %s", entityName);
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void undeployEntity(String serviceEndpoint, String entityName, String entityId) {
        try {
            int responseCode = executeDelete(serviceEndpoint, entityId);
            if (responseCode == 404) {
                System.out.println(String.format("%s not found", StringUtils.capitalize(entityName)));
            } else if (responseCode == 204) {
                System.out.println(String.format("Successfully un-deployed %s", entityName));
            }
        } catch (Exception e) {
            String message = String.format("Error in un-deploying %s", entityName);
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void updateEntity(String serviceEndpoint, String entityBody, String entityName) {
        try {
            int responseCode = executePut(serviceEndpoint, entityBody);
            if (responseCode == 404) {
                System.out.println(String.format("%s not found", StringUtils.capitalize(entityName)));
            } else if (responseCode == 200) {
                System.out.println(String.format("Successfully updated %s", entityName));
            }
        } catch (Exception e) {
            String message = String.format("Error in updating %s", entityName);
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public void deleteEntity(String serviceEndpoint, String identifier, String entityName) {
        try {
            int responseCode = executeDelete(serviceEndpoint, identifier);
            if (responseCode == 200) {
                System.out.println(String.format("Successfully deleted %s", entityName));
            }
        } catch (Exception e) {
            String message = String.format("Error in deleting %s", entityName);
            System.out.println(message);
            logger.error(message, e);
        }
    }

    public Object getEntity(String serviceEndpoint, Class responseJsonClass, String identifier, String entityName) {
        try {
            return executeGet(serviceEndpoint.replace("{id}", identifier), responseJsonClass);
        } catch (Exception e) {
            String message = String.format("Error in getting %s", entityName);
            System.out.println(message);
            logger.error(message, e);
            return null;
        }
    }

    public Object listEntity(String serviceEndpoint, Class responseJsonClass, String entityName) {
        try {
            return executeGet(serviceEndpoint, responseJsonClass);
        } catch (Exception e) {
            String message = String.format("Error in listing %s", entityName);
            System.out.println(message);
            logger.error(message, e);
            return null;
        }
    }

    private int executePost(String serviceEndpoint, String postBody) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = doPost(httpClient, getBaseURL()+ serviceEndpoint, postBody);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode < 200 || responseCode >= 300) {
                CliUtils.printError(response);
            }
            return responseCode;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private Object executeGet(String serviceEndpoint, Class responseJsonClass) throws Exception {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpResponse response = null;

        try {
            response = doGet(httpClient, getBaseURL() + serviceEndpoint);
            int responseCode = response.getStatusLine().getStatusCode();

            if((responseCode >= 400) && (responseCode < 500)) {
                // Entity not found
                return null;
            } else if (responseCode < 200 || responseCode >= 300) {
                CliUtils.printError(response);
                return null;
            } else {
                String resultString = CliUtils.getHttpResponseString(response);
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                return gson.fromJson(resultString, responseJsonClass);
            }
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private int executePut(String serviceEndpoint, String postBody) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = doPut(httpClient, getBaseURL() + serviceEndpoint, postBody);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode < 200 || responseCode >= 300) {
                CliUtils.printError(response);
            }
            return responseCode;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    private int executeDelete(String serviceEndpoint, String identifier) throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            System.out.println(getBaseURL() + serviceEndpoint.replace("{id}", identifier));
            HttpResponse response = doDelete(httpClient, getBaseURL() + serviceEndpoint.replace("{id}", identifier));

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode < 200 || responseCode >= 300) {
                CliUtils.printError(response);
            }
            return responseCode;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}