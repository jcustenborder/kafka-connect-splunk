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

import com.google.inject.servlet.GuiceFilter;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class SplunkHttpSourceTask extends SourceTask {
  static final Logger log = LoggerFactory.getLogger(SplunkHttpSourceTask.class);
  SplunkHttpSourceConnectorConfig config;
  Server server;

  @Override
  public String version() {
    return VersionUtil.getVersion();
  }

  @Override
  public void start(Map<String, String> map) {
    this.config = new SplunkHttpSourceConnectorConfig(map);
    this.server = new Server(this.config.port());

    ServletContextHandler servletContextHandler = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);
    servletContextHandler.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
    servletContextHandler.addServlet(DefaultServlet.class, "/");

    try {
      server.start();
    } catch (Exception e) {
      throw new ConnectException("Exception thrown while starting server", e);
    }
  }

  @Override
  public List<SourceRecord> poll() throws InterruptedException {
    //TODO: Create SourceRecord objects that will be sent the kafka cluster.
    throw new UnsupportedOperationException("This has not been implemented.");
  }

  @Override
  public void stop() {
    //TODO: Do whatever is required to stop your task.
  }
}