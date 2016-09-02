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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.Json;
import io.confluent.kafka.connect.utils.data.SourceRecordConcurrentLinkedDeque;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;

public class EventServlet extends HttpServlet {
  private static final Logger log = LoggerFactory.getLogger(EventServlet.class);
  ServletConfig servletConfig;
  JsonFactory jsonFactory;
  ObjectMapper objectMapper;
  EventConverter converter;
  SourceRecordConcurrentLinkedDeque recordQueue;

  SplunkHttpSourceConnectorConfig config;
  Set<String> allowedIndexes;


  public void configure(SplunkHttpSourceConnectorConfig config, JsonFactory jsonFactory, ObjectMapper objectMapper, SourceRecordConcurrentLinkedDeque recordQueue) {
    this.config = config;
    this.jsonFactory = jsonFactory;
    this.objectMapper = objectMapper;
    this.converter = new EventConverter(this.objectMapper, this.config);
    this.recordQueue = recordQueue;
    this.allowedIndexes = this.config.allowedIndexes();
  }

  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    this.servletConfig = servletConfig;
  }

  @Override
  public ServletConfig getServletConfig() {
    return this.servletConfig;
  }

  public String host(HttpServletRequest request) {
    String ip;
    String xForwardedFor = request.getHeader("X-Forwarded-For");

    if (null != xForwardedFor) {
      ip = xForwardedFor.split("\\s*,\\s*")[0];
    } else {
      ip = request.getRemoteAddr();
    }

    return ip;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (log.isInfoEnabled()) {
      log.info("Reading message body.");
    }

    response.setHeader("X-Content-Type-Options", "nosniff");
    response.setHeader("X-Frame-Options", "SAMEORIGIN");
    response.setCharacterEncoding("UTF-8");
    response.setContentType(Json.MEDIA_TYPE);

    String remoteHost = host(request);

    try {

      try (BufferedReader bodyReader = request.getReader()) {
        try (EventIterator iterator = EventIterator.create(this.objectMapper, this.jsonFactory, bodyReader)) {

          while (iterator.hasNext()) {
            JsonNode jsonNode = iterator.next();

            if (log.isDebugEnabled()) {
              log.debug("Message received {}", jsonNode);
            }

            if (!this.allowedIndexes.isEmpty() && jsonNode.has("index")) {
              JsonNode indexNode = jsonNode.get("index");
              String index = indexNode.asText();

              if (!allowedIndexes.contains(index)) {

              }
            }

            SourceRecord sourceRecord = this.converter.convert(jsonNode, remoteHost);
            this.recordQueue.push(sourceRecord);
          }
        }
      }
      response.setStatus(200);

    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("Exception thrown", ex);
      }

      response.setStatus(500);
    }
  }

  @Override
  public String getServletInfo() {
    return null;
  }

  @Override
  public void destroy() {

  }
}
