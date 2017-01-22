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
package com.github.jcustenborder.kafka.connect.splunk;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SplunkHttpSourceConnectorConfig extends AbstractConfig {

  public static final String PORT_CONF = "splunk.port";
  public static final String KEYSTORE_PATH_CONF = "splunk.ssl.key.store.path";
  public static final String KEYSTORE_PASSWORD_CONF = "splunk.ssl.key.store.password";
  public static final String SSL_RENEGOTIATION_ALLOWED_CONF = "splunk.ssl.renegotiation.allowed";
  public static final String EVENT_COLLECTOR_URL_CONF = "splunk.collector.url";
  public static final String EVENT_COLLECTOR_INDEX_ALLOWED_CONF = "splunk.collector.index.allowed";
  public static final String EVENT_COLLECTOR_INDEX_DEFAULT_CONF = "splunk.collector.index.default";
  public static final String TOPIC_PREFIX_CONF = "kafka.topic";
  public static final String TOPIC_PER_INDEX_CONF = "topic.per.index";
  public static final String BATCH_SIZE_CONF = "batch.size";
  public static final String BACKOFF_MS_CONF = "backoff.ms";
  private static final String PORT_DOC = "The port to configure the http listener on.";
  private static final String KEYSTORE_PATH_DOC = "The path to the keystore on the local filesystem.";
  private static final String KEYSTORE_PASSWORD_DOC = "The password for opening the keystore.";
  private static final String SSL_RENEGOTIATION_ALLOWED_DOC = "Flag to determine if ssl renegotiation is allowed.";
  private static final String EVENT_COLLECTOR_URL_DOC = "Path fragement the servlet should respond on";
  private static final String EVENT_COLLECTOR_INDEX_ALLOWED_DOC = "The indexes this connector allows data to be" +
      " written for. Specifying an index outside of this list will result in an exception being raised.";
  private static final String EVENT_COLLECTOR_INDEX_DEFAULT_DOC = "The index that will be used if no index is " +
      "specified in the event message.";
  private static final String TOPIC_PREFIX_DOC = "This value contains the topic that the messages will be written to. " +
      "If topic per index is enabled this will be the prefix for the topic. If not this will be the exact topic.";
  private static final String TOPIC_PER_INDEX_DOC = "Flag determines if the all generated messages should be written to" +
      "a single topic or should the messages be placed in a topic prefixed by the supplied index. If true the `" +
      TOPIC_PREFIX_CONF + "` setting will be concatenated along with the index name. If false the `" + TOPIC_PREFIX_CONF
      + "` value will be used for the topic.";

  private static final String BATCH_SIZE_DOC = "Maximum number of records to write per poll call.";
  private static final String BACKOFF_MS_DOC = "The number of milliseconds to back off when there are no records in the" +
      "queue.";

  public SplunkHttpSourceConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
  }

  public SplunkHttpSourceConnectorConfig(Map<String, String> parsedConfig) {
    this(conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
        .define(PORT_CONF, Type.INT, 8088, Importance.HIGH, PORT_DOC)
        .define(SSL_RENEGOTIATION_ALLOWED_CONF, Type.BOOLEAN, true, Importance.LOW, SSL_RENEGOTIATION_ALLOWED_DOC)
        .define(KEYSTORE_PATH_CONF, Type.STRING, Importance.HIGH, KEYSTORE_PATH_DOC)
        .define(KEYSTORE_PASSWORD_CONF, Type.PASSWORD, Importance.HIGH, KEYSTORE_PASSWORD_DOC)
        .define(EVENT_COLLECTOR_URL_CONF, Type.STRING, "/services/collector/event", Importance.LOW, EVENT_COLLECTOR_URL_DOC)
        .define(EVENT_COLLECTOR_INDEX_ALLOWED_CONF, Type.LIST, new ArrayList<>(), Importance.LOW, EVENT_COLLECTOR_INDEX_ALLOWED_DOC)
        .define(EVENT_COLLECTOR_INDEX_DEFAULT_CONF, Type.STRING, Importance.HIGH, EVENT_COLLECTOR_INDEX_DEFAULT_DOC)
        .define(TOPIC_PER_INDEX_CONF, Type.BOOLEAN, false, Importance.MEDIUM, TOPIC_PER_INDEX_DOC)
        .define(TOPIC_PREFIX_CONF, Type.STRING, Importance.HIGH, TOPIC_PREFIX_DOC)
        .define(BATCH_SIZE_CONF, Type.INT, 10000, Importance.LOW, BATCH_SIZE_DOC)
        .define(BACKOFF_MS_CONF, Type.INT, 100, Importance.LOW, BACKOFF_MS_DOC);
  }

  public int port() {
    return this.getInt(PORT_CONF);
  }

  public String keyStorePath() {
    return this.getString(KEYSTORE_PATH_CONF);
  }

  public String keyStorePassword() {
    return this.getPassword(KEYSTORE_PASSWORD_CONF).value();
  }

  public boolean sslRenegotiationAllowed() {
    return this.getBoolean(SSL_RENEGOTIATION_ALLOWED_CONF);
  }

  public String eventCollectorUrl() {
    return this.getString(EVENT_COLLECTOR_URL_CONF);
  }

  public SslContextFactory sslContextFactory() {
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(keyStorePath());
    sslContextFactory.setKeyStorePassword(keyStorePassword());
    sslContextFactory.setRenegotiationAllowed(sslRenegotiationAllowed());
    return sslContextFactory;
  }

  public Set<String> allowedIndexes() {
    return new HashSet<>(this.getList(EVENT_COLLECTOR_INDEX_ALLOWED_CONF));
  }

  public String defaultIndex() {
    return this.getString(EVENT_COLLECTOR_INDEX_DEFAULT_CONF);
  }

  public boolean topicPerIndex() {
    return this.getBoolean(TOPIC_PER_INDEX_CONF);
  }

  public String topicPrefix() {
    return this.getString(TOPIC_PREFIX_CONF);
  }

  public int batchSize() {
    return this.getInt(BATCH_SIZE_CONF);
  }

  public int backoffMS() {
    return this.getInt(BACKOFF_MS_CONF);
  }

}
