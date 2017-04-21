/**
 * Copyright © 2016 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jcustenborder.kafka.connect.splunk;

import com.fasterxml.jackson.core.JsonFactory;
import com.github.jcustenborder.kafka.connect.utils.VersionUtil;
import com.github.jcustenborder.kafka.connect.utils.data.SourceRecordConcurrentLinkedDeque;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SplunkHttpSourceTask extends SourceTask {
  static final Logger log = LoggerFactory.getLogger(SplunkHttpSourceTask.class);
  SplunkHttpSourceConnectorConfig config;
  Server server;
  EventServlet eventServlet;
  SourceRecordConcurrentLinkedDeque sourceRecordConcurrentLinkedDeque;

  @Override
  public String version() {
    return VersionUtil.version(this.getClass());
  }

  @Override
  public void start(Map<String, String> map) {
    this.config = new SplunkHttpSourceConnectorConfig(map);
    this.server = new Server();

    HttpConfiguration httpsConfiguration = new HttpConfiguration();
    httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

    SslContextFactory sslContextFactory = this.config.sslContextFactory();
    ServerConnector sslConnector = new ServerConnector(
        server,
        new SslConnectionFactory(sslContextFactory, "http/1.1"),
        new HttpConnectionFactory(httpsConfiguration)
    );
    sslConnector.setPort(this.config.port);

    server.setConnectors(new ServerConnector[]{sslConnector});

    log.info("Configuring Splunk Event Collector Servlet for {}", this.config.eventCollectorUrl);

    ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.NO_SESSIONS);
    servletContextHandler.addServlet(DefaultServlet.class, "/");
    ServletHolder holder = servletContextHandler.addServlet(EventServlet.class, this.config.eventCollectorUrl);

    this.sourceRecordConcurrentLinkedDeque = new SourceRecordConcurrentLinkedDeque(this.config.batchSize, this.config.backoffMS);

    try {
      log.info("Starting web server on port {}", this.config.port);
      server.start();
    } catch (Exception e) {
      throw new IllegalStateException("Exception thrown while starting server", e);
    }

    try {
      this.eventServlet = (EventServlet) holder.ensureInstance();
    } catch (ServletException e) {
      throw new ConnectException("This is really broken", e);
    }

    JsonFactory jsonFactory = new JsonFactory();

    this.eventServlet.configure(this.config, jsonFactory, this.sourceRecordConcurrentLinkedDeque);
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    List<SourceRecord> records = new ArrayList<>(this.config.batchSize);

    while (!this.sourceRecordConcurrentLinkedDeque.drain(records)) {
      log.trace("No records received. Sleeping.");
    }

    return records;
  }

  @Override
  public void stop() {
    try {
      this.server.stop();
    } catch (Exception e) {
      log.error("Exception thrown calling server.stop()", e);
    }
  }
}