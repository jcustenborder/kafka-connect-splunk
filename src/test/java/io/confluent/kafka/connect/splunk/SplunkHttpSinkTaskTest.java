/**
 * Copyright (C) ${project.inceptionYear} Jeremy Custenborder (jcustenborder@gmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.kafka.connect.splunk;

import com.google.api.client.http.LowLevelHttpRequest;
import com.google.api.client.http.LowLevelHttpResponse;
import com.google.api.client.json.Json;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class SplunkHttpSinkTaskTest {

  SplunkHttpSinkTask task;

  @Before
  public void setup() {
    this.task = new SplunkHttpSinkTask();
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(SplunkHttpSinkConnectorConfig.REMOTE_HOST_CONF, "127.0.0.1");
    settings.put(SplunkHttpSinkConnectorConfig.AUTHORIZATION_TOKEN_CONF, "B5A79AAD-D822-46CC-80D1-819F80D7BFB0");
    settings.put(SplunkHttpSinkConnectorConfig.CURL_LOGGING_ENABLED_CONF, Boolean.TRUE.toString());
    this.task.start(settings);
  }

  LowLevelHttpResponse getResponse(int statusCode) throws IOException {
    String resourceFile = String.format("response.%s.json", statusCode);
    InputStream resourceStream = SplunkHttpSinkTaskTest.class.getResourceAsStream(resourceFile);
    Preconditions.checkNotNull(resourceStream, "Resource %s could not be found", resourceFile);

    LowLevelHttpResponse httpResponse = mock(LowLevelHttpResponse.class, CALLS_REAL_METHODS);
    when(httpResponse.getStatusCode()).thenReturn(statusCode);
    when(httpResponse.getContentType()).thenReturn(Json.MEDIA_TYPE);
    when(httpResponse.getContent()).thenReturn(resourceStream);
    when(httpResponse.getContentEncoding()).thenReturn("UTF-8");
    return httpResponse;
  }

  @Test
  public void normal() throws IOException {
    Collection<SinkRecord> sinkRecords = new ArrayList<>();
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main"));

    final LowLevelHttpRequest httpRequest = mock(LowLevelHttpRequest.class, CALLS_REAL_METHODS);
    LowLevelHttpResponse httpResponse = getResponse(200);
    when(httpRequest.execute()).thenReturn(httpResponse);

    this.task.transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return httpRequest;
      }
    };

    this.task.httpRequestFactory = this.task.transport.createRequestFactory(this.task.httpRequestInitializer);
    this.task.put(sinkRecords);
  }

  @Test(expected = RetriableException.class)
  public void connectionRefused() throws IOException {
    Collection<SinkRecord> sinkRecords = new ArrayList<>();
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main"));

    final LowLevelHttpRequest httpRequest = mock(LowLevelHttpRequest.class, CALLS_REAL_METHODS);
    when(httpRequest.execute()).thenThrow(ConnectException.class);
    this.task.transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return httpRequest;
      }
    };

    this.task.httpRequestFactory = this.task.transport.createRequestFactory(this.task.httpRequestInitializer);
    this.task.put(sinkRecords);
  }

  @Test(expected = org.apache.kafka.connect.errors.ConnectException.class)
  public void contentLengthTooLarge() throws IOException {
    Collection<SinkRecord> sinkRecords = new ArrayList<>();
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main"));

    final LowLevelHttpRequest httpRequest = mock(LowLevelHttpRequest.class, CALLS_REAL_METHODS);
    LowLevelHttpResponse httpResponse = getResponse(417);
    when(httpResponse.getContentType()).thenReturn("text/html");
    when(httpRequest.execute()).thenReturn(httpResponse);

    this.task.transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return httpRequest;
      }
    };

    this.task.httpRequestFactory = this.task.transport.createRequestFactory(this.task.httpRequestInitializer);
    this.task.put(sinkRecords);
  }

  @Test(expected = org.apache.kafka.connect.errors.ConnectException.class)
  public void invalidToken() throws IOException {
    Collection<SinkRecord> sinkRecords = new ArrayList<>();
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main"));

    final LowLevelHttpRequest httpRequest = mock(LowLevelHttpRequest.class, CALLS_REAL_METHODS);
    LowLevelHttpResponse httpResponse = getResponse(403);
    when(httpRequest.execute()).thenReturn(httpResponse);

    this.task.transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return httpRequest;
      }
    };

    this.task.httpRequestFactory = this.task.transport.createRequestFactory(this.task.httpRequestInitializer);
    this.task.put(sinkRecords);
  }

  @Test(expected = org.apache.kafka.connect.errors.ConnectException.class)
  public void invalidIndex() throws IOException {
    Collection<SinkRecord> sinkRecords = new ArrayList<>();
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp"));
    SinkRecordContentTest.addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main"));

    final LowLevelHttpRequest httpRequest = mock(LowLevelHttpRequest.class, CALLS_REAL_METHODS);
    LowLevelHttpResponse httpResponse = getResponse(400);
    when(httpRequest.execute()).thenReturn(httpResponse);

    this.task.transport = new MockHttpTransport() {
      @Override
      public LowLevelHttpRequest buildRequest(String method, String url) throws IOException {
        return httpRequest;
      }
    };

    this.task.httpRequestFactory = this.task.transport.createRequestFactory(this.task.httpRequestInitializer);
    this.task.put(sinkRecords);
  }

}
