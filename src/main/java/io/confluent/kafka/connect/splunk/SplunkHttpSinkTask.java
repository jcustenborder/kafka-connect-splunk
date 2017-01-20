/**
 * Copyright © 2016 Jeremy Custenborder (jcustenborder@gmail.com)
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

import com.google.api.client.http.GZipEncoding;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;

public class SplunkHttpSinkTask extends SinkTask {
  static final HttpMediaType JSON_MEDIA_TYPE = new HttpMediaType(Json.MEDIA_TYPE);
  private static Logger log = LoggerFactory.getLogger(SplunkHttpSinkTask.class);
  SplunkHttpSinkConnectorConfig config;
  HttpTransport transport;
  JsonFactory jsonFactory = new JacksonFactory();
  HttpRequestFactory httpRequestFactory;
  HttpRequestInitializer httpRequestInitializer;
  GenericUrl eventCollectorUrl;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> map) {
    this.config = new SplunkHttpSinkConnectorConfig(map);

    java.util.logging.Logger logger = java.util.logging.Logger.getLogger(HttpTransport.class.getName());
    logger.addHandler(new RequestLoggingHandler(log));
    if (this.config.curlLoggingEnabled()) {
      logger.setLevel(Level.ALL);
    } else {
      logger.setLevel(Level.WARNING);
    }

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

    final String userAgent = String.format("kafka-connect-splunk/%s", VersionUtil.getVersion());
    final boolean curlLogging = this.config.curlLoggingEnabled();
    this.httpRequestInitializer = new HttpRequestInitializer() {
      @Override
      public void initialize(HttpRequest httpRequest) throws IOException {
        httpRequest.getHeaders().setAuthorization(authHeaderValue);
        httpRequest.getHeaders().setAccept(Json.MEDIA_TYPE);
        httpRequest.getHeaders().setUserAgent(userAgent);
        httpRequest.setParser(jsonObjectParser);
        httpRequest.setEncoding(new GZipEncoding());
        httpRequest.setThrowExceptionOnExecuteError(false);
        httpRequest.setConnectTimeout(config.connectTimeout());
        httpRequest.setReadTimeout(config.readTimeout());
        httpRequest.setCurlLoggingEnabled(curlLogging);
//        httpRequest.setLoggingEnabled(curlLogging);
      }
    };

    this.httpRequestFactory = this.transport.createRequestFactory(this.httpRequestInitializer);

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
    if (collection.isEmpty()) {
      if (log.isDebugEnabled()) {
        log.debug("No records in collection.");
      }
      return;
    }

    try {
      if (log.isDebugEnabled()) {
        log.debug("Posting {} message(s) to {}", collection.size(), this.eventCollectorUrl);
      }

      SinkRecordContent sinkRecordContent = new SinkRecordContent(collection);

      if (log.isDebugEnabled()) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
          sinkRecordContent.writeTo(outputStream);
          outputStream.flush();
          byte[] buffer = outputStream.toByteArray();
          log.debug("Posting\n{}", new String(buffer, "UTF-8"));
        } catch (IOException ex) {
          if (log.isDebugEnabled()) {
            log.debug("exception thrown while previewing post", ex);
          }
        }
      }

      HttpRequest httpRequest = this.httpRequestFactory.buildPostRequest(this.eventCollectorUrl, sinkRecordContent);
      HttpResponse httpResponse = httpRequest.execute();

      if (httpResponse.getStatusCode() == 403) {
        throw new ConnectException("Authentication was not successful. Please check the token with Splunk.");
      }

      if (httpResponse.getStatusCode() == 417) {
        if (log.isWarnEnabled()) {
          log.warn("This exception happens when too much content is pushed to splunk per call. Look at this blog post " +
              "http://blogs.splunk.com/2016/08/12/handling-http-event-collector-hec-content-length-too-large-errors-without-pulling-your-hair-out/" +
              " Setting consumer.max.poll.records to a lower value will decrease the number of message posted to Splunk " +
              "at once.");
        }
        throw new ConnectException("Status 417: Content-Length of XXXXX too large (maximum is 1000000). Verify Splunk config or " +
            " lower the value in consumer.max.poll.records.");
      }

      if (JSON_MEDIA_TYPE.equalsIgnoreParameters(httpResponse.getMediaType())) {
        SplunkStatusMessage statusMessage = httpResponse.parseAs(SplunkStatusMessage.class);

        if (!statusMessage.isSuccessful()) {
          throw new RetriableException(statusMessage.toString());
        }
      } else {
        throw new RetriableException("Media type of " + Json.MEDIA_TYPE + " was not returned.");
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
