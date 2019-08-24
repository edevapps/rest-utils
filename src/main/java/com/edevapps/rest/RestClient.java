/*
 *     Copyright (c) 2018, The Eduard Burenkov (http://edevapps.com)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.edevapps.rest;

import static com.edevapps.rest.RestClient.ResponseStatusCode.NOT_FOUND;
import static com.edevapps.rest.RestClient.ResponseStatusCode.UNAUTHORIZED;
import static com.edevapps.util.ObjectsUtil.requireNonNull;
import static javax.ws.rs.core.MediaType.WILDCARD;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.io.IOException;
import java.util.Map;
import javax.ws.rs.core.MultivaluedMap;
import org.codehaus.jackson.map.ObjectMapper;

public class RestClient {

  public enum UriScheme {
    http,
    https,
  }

  public static class ResponseStatusCode {

    public static final int OK = 200;
    public static final int UNAUTHORIZED = 401;
    public static final int NOT_FOUND = 404;
  }

  private UriScheme scheme;
  private String host;
  private int port;
  private String homeTarget;
  private String user;
  private String password;
  private final Client client;

  public RestClient(UriScheme scheme, String host, int port,
      String homeTarget, String user, String password) {
    this.scheme = requireNonNull(scheme, "scheme");
    this.host = requireNonNull(host, "host");
    this.port = port;
    this.homeTarget = homeTarget;
    this.user = user;
    this.password = password;
    this.client = buildClient();
  }

  private Client buildClient() {
    Client client = Client.create();
    addFilters(client);
    return client;
  }

  private void addFilters(Client client) {
    if(this.user != null && this.password != null) {
      client.addFilter(new HTTPBasicAuthFilter(this.user, this.password));
    }
  }

  public UriScheme getScheme() {
    return scheme;
  }

  public void setScheme(UriScheme scheme) {
    this.scheme = requireNonNull(scheme, "scheme");
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = requireNonNull(host, "host");
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = requireNonNull(port, "port");
  }

  public String getHomeTarget() {
    return homeTarget;
  }

  public void setHomeTarget(String homeTarget) {
    this.homeTarget = requireNonNull(homeTarget, "homeTarget");
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  private String buildTarget() {
    StringBuilder stringBuilder = new StringBuilder(this.scheme.toString() + "://" + this.host);
    if (this.port >= 0) {
      stringBuilder.append(":").append(port);
    }
    stringBuilder.append(this.homeTarget);
    return stringBuilder.toString();
  }

  private WebResource resourceFrom(String path) {
    return this.client.resource(buildTarget() + path);
  }

  private boolean hasError(int status) {
    return status >= 400;
  }

  private void throwError(int statusCode) {
    switch (statusCode) {
      case UNAUTHORIZED: {
        throw new UnauthorizedResponseException("Invalid user name or password");
      }
      case NOT_FOUND: {
        throw new NotFoundResponseException("Resource is not found.");
      }
      default: {
        throw new ResponseException("Unknown error.");
      }
    }
  }

  private <T> T mapToObject(Class<T> resultType, String source) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(source, resultType);
    } catch (IOException ex) {
      throw new IllegalArgumentException(ex);
    }

  }

  public <T> T getObject(Class<T> resultType, String path) {
    ClientResponse response = client.resource(buildTarget() + path)
        .type(WILDCARD)
        .accept(WILDCARD)
        .get(ClientResponse.class);
    if (hasError(response.getStatus())) {
      throwError(response.getStatus());
    }
    return mapToObject(resultType, response.getEntity(String.class));
  }

  public <T> T postObject(Class<T> resultType, String path) {
    WebResource webResource = resourceFrom(path);
    ClientResponse response = webResource
        .type(WILDCARD)
        .accept(WILDCARD)
        .post(ClientResponse.class);
    if (hasError(response.getStatus())) {
      throwError(response.getStatus());
    }
    return mapToObject(resultType, response.getEntity(String.class));
  }

  private MultivaluedMap<String, String> buildParams(Map<String, String> params) {
    MultivaluedMap<String, String> result = new MultivaluedMapImpl();
    for (Map.Entry<String, String> parameter : params.entrySet()) {
      result.add(parameter.getKey(), parameter.getValue());
    }
    return result;
  }

  public <T> T getObject(Class<T> resultType, String path, Map<String, String> params) {
    WebResource webResource = resourceFrom(path);
    ClientResponse response = webResource
        .queryParams(buildParams(params))
        .type(WILDCARD)
        .accept(WILDCARD)
        .get(ClientResponse.class);
    if (hasError(response.getStatus())) {
      throwError(response.getStatus());
    }
    return mapToObject(resultType, response.getEntity(String.class));
  }

  public <T> T postObject(Class<T> resultType, String path, Map<String, String> params) {
    WebResource webResource = resourceFrom(path);
    ClientResponse response = webResource
        .queryParams(buildParams(params))
        .type(WILDCARD)
        .accept(WILDCARD)
        .post(ClientResponse.class);
    if (hasError(response.getStatus())) {
      throwError(response.getStatus());
    }
    return mapToObject(resultType, response.getEntity(String.class));
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private UriScheme scheme;
    private String host;
    private int port = -1;
    private String homeTarget;
    private String user;
    private String password;

    public Builder() {
    }

    public Builder setScheme(UriScheme scheme) {
      this.scheme = scheme;
      return this;
    }

    public Builder setHost(String host) {
      this.host = host;
      return this;
    }

    public Builder setPort(int port) {
      this.port = port;
      return this;
    }

    public Builder setHomeTarget(String homeTarget) {
      this.homeTarget = homeTarget;
      return this;
    }

    public Builder setUser(String user) {
      this.user = user;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

    public RestClient build() {
      return new RestClient(this.scheme, this.host, this.port, this.homeTarget, this.user,
          this.password);
    }
  }
}
