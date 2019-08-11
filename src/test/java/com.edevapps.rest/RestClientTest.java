package com.edevapps.rest;

import static org.junit.Assert.assertEquals;

import com.edevapps.rest.RestClient.UriScheme;
import org.junit.Test;

public class RestClientTest {

  @Test
  public void httpsConnectionTest() {
    RestClient client = RestClient.builder()
        .setScheme(UriScheme.https)
        .setHost("jsonplaceholder.typicode.com")
        .setHomeTarget("/posts")
        .build();

    TypicodeComPostDto testDto = client.getObject(TypicodeComPostDto.class, "/1");

    assertEquals(testDto.getId(), "1");
  }
}
