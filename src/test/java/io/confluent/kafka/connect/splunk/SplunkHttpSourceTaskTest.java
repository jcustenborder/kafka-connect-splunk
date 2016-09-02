/**
 * Copyright © 2016 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.kafka.connect.splunk;

import com.google.api.client.json.Json;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class SplunkHttpSourceTaskTest {
  final long TIME = 1472266250342L;
  SplunkHttpSourceTask task;
  String TOPIC_PREFIX_CONF = "splunk";

  static BufferedReader records(Map<String, ?>... records) throws IOException {
    List<SinkRecord> sinkRecords = new ArrayList<>();
    for (Map<String, ?> record : records) {
      SinkRecordContentTest.addRecord(sinkRecords, record);
    }
    byte[] buffer;

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      SinkRecordContent content = new SinkRecordContent(
          ObjectMapperFactory.create(),
          sinkRecords);
      content.writeTo(outputStream);
      buffer = outputStream.toByteArray();
    }

    InputStream inputStream = new ByteArrayInputStream(buffer);

    return new BufferedReader(new InputStreamReader(inputStream));
  }

  @Before
  public void setup() {
    this.task = new SplunkHttpSourceTask();
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(SplunkHttpSourceConnectorConfig.TOPIC_PREFIX_CONF, TOPIC_PREFIX_CONF);
    settings.put(SplunkHttpSourceConnectorConfig.KEYSTORE_PASSWORD_CONF, "password");
    settings.put(SplunkHttpSourceConnectorConfig.KEYSTORE_PATH_CONF, SplunkHttpSourceTaskTest.class.getResource("keystore.jks").toExternalForm());
    settings.put(SplunkHttpSourceConnectorConfig.EVENT_COLLECTOR_INDEX_DEFAULT_CONF, "default");
    this.task.start(settings);
    assertNotNull(this.task.eventServlet);
    assertNotNull(this.task.eventServlet.objectMapper);
    assertNotNull(this.task.eventServlet.config);
    assertNotNull(this.task.eventServlet.converter);
  }

  @After
  public void teardown() {
    this.task.stop();
  }

  void verifyResponse(final HttpServletResponse response, final int status, final String contentType) {
    verify(response).setStatus(status);
    verify(response).setContentType(contentType);
    verify(response).setHeader("X-Content-Type-Options", "nosniff");
    verify(response).setHeader("X-Frame-Options", "SAMEORIGIN");
    verify(response).setCharacterEncoding("UTF-8");
  }
//  InputStream

  @Test
  public void simple() throws ServletException, IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    BufferedReader reader = records(
        ImmutableMap.of(
            "time", new Date(TIME),
            "host", "localhost",
            "source", "datasource",
            "sourcetype", "txt",
            "event", "Hello world!"
        )
    );

    when(request.getReader()).thenReturn(reader);
    this.task.eventServlet.doPost(request, response);
    verifyResponse(response, 200, Json.MEDIA_TYPE);
    assertEquals("Size does not match.", 1, this.task.sourceRecordConcurrentLinkedDeque.size());
  }

  private HttpServletRequest mockRequest(BufferedReader reader) throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getReader()).thenReturn(reader);
    when(request.getHeader("X-Forwarded-For")).thenReturn("10.20.30.40");
    when(request.getRemoteAddr()).thenReturn("10.20.51.71");
    return request;
  }

  @Test
  public void host() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader("X-Forwarded-For")).thenReturn("10.20.30.40, 10.10.10.10, 10.10.0.1");
    when(request.getRemoteAddr()).thenReturn("10.20.51.71");
    String ipAddress = this.task.eventServlet.host(request);
    assertEquals("Host should have returned X-Forwarded-For", "10.20.30.40", ipAddress);

    request = mock(HttpServletRequest.class);
    when(request.getRemoteAddr()).thenReturn("10.20.51.71");
    ipAddress = this.task.eventServlet.host(request);
    assertEquals("Host should have returned RemoteAddr", "10.20.51.71", ipAddress);
  }

  @Test
  public void emptyContent() throws ServletException, IOException {

    HttpServletResponse response = mock(HttpServletResponse.class);
    BufferedReader reader = records(

    );
    HttpServletRequest request = mockRequest(reader);
    when(request.getReader()).thenReturn(reader);
    this.task.eventServlet.doPost(request, response);
    verifyResponse(response, 200, Json.MEDIA_TYPE);
  }
}