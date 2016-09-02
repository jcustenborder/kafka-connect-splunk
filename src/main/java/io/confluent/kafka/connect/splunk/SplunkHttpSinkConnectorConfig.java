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

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;


public class SplunkHttpSinkConnectorConfig extends AbstractConfig {

  public static final String AUTHORIZATION_TOKEN_CONF = "splunk.auth.token";
  public static final String REMOTE_PORT_CONF = "splunk.remote.port";
  public static final String REMOTE_HOST_CONF = "splunk.remote.host";
  public static final String SSL_CONF = "splunk.ssl.enabled";
  public static final String SSL_VALIDATE_CERTIFICATES_CONF = "splunk.ssl.validate.certs";
  public static final String SSL_TRUSTSTORE_PATH_CONF = "splunk.ssl.trust.store.path";
  public static final String SSL_TRUSTSTORE_PASSWORD_CONF = "splunk.ssl.trust.store.password";
  public static final String CONNECT_TIMEOUT_CONF = "splunk.connect.timeout.ms";
  public static final String READ_TIMEOUT_CONF = "splunk.read.timeout.ms";
  public static final String CURL_LOGGING_ENABLED_CONF = "splunk.curl.logging.enabled";

  static final String AUTHORIZATION_TOKEN_DOC = "The authorization token to use when writing data to splunk.";
  static final String REMOTE_PORT_DOC = "Port on the remote splunk server to write to.";
  static final String REMOTE_HOST_DOC = "The hostname of the remote splunk host to write data do.";
  static final String SSL_DOC = "Flag to determine if the connection to splunk should be over ssl.";
  static final String SSL_VALIDATE_CERTIFICATES_DOC = "Flag to determine if ssl connections should validate the certificate" +
      "of the remote host.";
  static final String SSL_TRUSTSTORE_PATH_DOC = "Path on the local disk to the certificate trust store.";
  static final String SSL_TRUSTSTORE_PASSWORD_DOC = "Password for the trust store.";
  static final String CONNECT_TIMEOUT_DOC = "The maximum amount of time for a connection to be established.";
  static final String READ_TIMEOUT_DOC = "Sets the timeout in milliseconds to read data from an established connection " +
      "or 0 for an infinite timeout.";
  static final String CURL_LOGGING_ENABLED_DOC = "Flag to determine if requests to Splunk should be logged in curl form." +
      " This will output a curl command to replicate the call to Splunk.";

  public SplunkHttpSinkConnectorConfig(ConfigDef config, Map<String, String> parsedConfig) {
    super(config, parsedConfig);
  }

  public SplunkHttpSinkConnectorConfig(Map<String, String> parsedConfig) {
    this(conf(), parsedConfig);
  }

  public static ConfigDef conf() {
    return new ConfigDef()
        .define(AUTHORIZATION_TOKEN_CONF, Type.PASSWORD, Importance.HIGH, AUTHORIZATION_TOKEN_DOC)
        .define(REMOTE_HOST_CONF, Type.STRING, Importance.HIGH, REMOTE_HOST_DOC)
        .define(REMOTE_PORT_CONF, Type.INT, 8088, Importance.MEDIUM, REMOTE_PORT_DOC)
        .define(SSL_CONF, Type.BOOLEAN, true, Importance.HIGH, SSL_DOC)
        .define(SSL_VALIDATE_CERTIFICATES_CONF, Type.BOOLEAN, true, Importance.MEDIUM, SSL_VALIDATE_CERTIFICATES_DOC)
        .define(SSL_TRUSTSTORE_PATH_CONF, Type.STRING, "", Importance.HIGH, SSL_TRUSTSTORE_PATH_DOC)
        .define(SSL_TRUSTSTORE_PASSWORD_CONF, Type.PASSWORD, "", Importance.HIGH, SSL_TRUSTSTORE_PASSWORD_DOC)
        .define(CONNECT_TIMEOUT_CONF, Type.INT, 20000, Importance.LOW, CONNECT_TIMEOUT_DOC)
        .define(READ_TIMEOUT_CONF, Type.INT, 30000, Importance.LOW, READ_TIMEOUT_DOC)
        .define(CURL_LOGGING_ENABLED_CONF, Type.BOOLEAN, false, Importance.LOW, CURL_LOGGING_ENABLED_DOC);
  }

  public String authToken() {
    return this.getPassword(AUTHORIZATION_TOKEN_CONF).value();
  }


  public int splunkPort() {
    return this.getInt(REMOTE_PORT_CONF);
  }

  public String splunkHost() {
    return this.getString(REMOTE_HOST_CONF);
  }

  public boolean ssl() {
    return this.getBoolean(SSL_CONF);
  }

  public boolean validateCertificates() {
    return this.getBoolean(SSL_VALIDATE_CERTIFICATES_CONF);
  }

  public String trustStorePath() {
    return this.getString(SSL_TRUSTSTORE_PATH_CONF);
  }

  public boolean hasTrustStorePath() {
    return null != trustStorePath() && !trustStorePath().isEmpty();
  }

  public String trustStorePassword() {
    return this.getPassword(SSL_TRUSTSTORE_PASSWORD_CONF).toString();
  }

  public int connectTimeout() {
    return this.getInt(CONNECT_TIMEOUT_CONF);
  }

  public int readTimeout() {
    return this.getInt(READ_TIMEOUT_CONF);
  }

  public boolean curlLoggingEnabled() {
    return this.getBoolean(CURL_LOGGING_ENABLED_CONF);
  }

}
