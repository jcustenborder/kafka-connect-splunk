/**
 * Copyright (C) ${project.inceptionYear} Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.kafka.connect.splunk;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Map;

public class SplunkHttpSinkTask extends SinkTask {
  private static Logger log = LoggerFactory.getLogger(SplunkHttpSinkTask.class);
  SplunkHttpSinkConnectorConfig config;
  HttpTransport transport;
  JsonFactory jsonFactory = new JacksonFactory();
  HttpRequestFactory httpRequestFactory;
  GenericUrl eventCollectorUrl;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> map) {
    this.config = new SplunkHttpSinkConnectorConfig(map);

    if (log.isInfoEnabled()) {
      log.info("Starting...");
    }

    NetHttpTransport.Builder transportBuilder = new NetHttpTransport.Builder();

    if (!this.config.validateCertificates()) {
      if (log.isWarnEnabled()) {
        log.warn("Disabling ssl certificate verification.");
      }
      try {
        transportBuilder.doNotValidateCertificate();
      } catch (GeneralSecurityException e) {
        throw new IllegalStateException("Exception thrown calling transportBuilder.doNotValidateCertificate()", e);
      }
    }

    if (this.config.hasTrustStorePath()) {
      if (log.isInfoEnabled()) {
        log.info("Loading trust store from {}.", this.config.trustStorePath());
      }
      try (FileInputStream inputStream = new FileInputStream(this.config.trustStorePath())) {
        transportBuilder.trustCertificatesFromJavaKeyStore(inputStream, this.config.trustStorePassword());
      } catch (GeneralSecurityException | IOException ex) {
        throw new IllegalStateException("Exception thrown while setting up trust certificates.", ex);
      }
    }

    this.transport = transportBuilder.build();

    final String authHeaderValue = String.format("Splunk %s", this.config.authToken());
    final JsonObjectParser jsonObjectParser = new JsonObjectParser(jsonFactory);
    this.httpRequestFactory = transport.createRequestFactory(new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        httpRequest.getHeaders().setAuthorization(authHeaderValue);
        httpRequest.setParser(jsonObjectParser);
      }
    });

    this.eventCollectorUrl = new GenericUrl();
    this.eventCollectorUrl.setRawPath("/services/collector/event");
    this.eventCollectorUrl.setPort(this.config.splunkPort());
    this.eventCollectorUrl.setHost(this.config.splunkHost());

    if (this.config.ssl()) {
      this.eventCollectorUrl.setScheme("https");
    } else {
      this.eventCollectorUrl.setScheme("http");
    }

    if (log.isInfoEnabled()) {
      log.info("Setting Splunk Http Event Collector Url to {}", this.eventCollectorUrl);
    }
  }


  @Override
  public void put(Collection<SinkRecord> collection) {
    try {
      if (log.isDebugEnabled()) {
        log.debug("Posting {} message(s) to {}", collection.size(), this.eventCollectorUrl);
      }

      HttpRequest httpRequest = this.httpRequestFactory.buildPostRequest(this.eventCollectorUrl, new SinkRecordContent(collection));
      HttpResponse httpResponse = httpRequest.execute();
      SplunkStatusMessage statusMessage = httpResponse.parseAs(SplunkStatusMessage.class);

      if (!statusMessage.isSuccessful()) {
        throw new RetriableException(statusMessage.text());
      }
    } catch (IOException e) {
      throw new RetriableException(
          String.format("Exception while posting data to %s.", this.eventCollectorUrl),
          e
      );
    }
  }

  @Override
  public void flush(Map<TopicPartition, OffsetAndMetadata> map) {

  }

  @Override
  public void stop() {
    if (log.isInfoEnabled()) {
      log.info("Stopping...");
    }
  }

}
