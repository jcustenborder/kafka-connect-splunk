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

import com.google.common.collect.ImmutableMap;
import io.confluent.kafka.connect.utils.config.MarkdownFormatter;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SplunkHttpSourceConnectorConfigTest {

  @Test
  public void sslContextFactory() {
    final String EXPECTED_KEYSTORE_PATH = "file:///private/tmp/testing.keystore";
    final String EXPECTED_KEYSTORE_PASSWORD = "lnr5o2qnafbhnbf";
    final Boolean EXPECTED_SSL_RENEGOTIATION_ALLOWED = false;

    Map<String, String> settings = ImmutableMap.of(
        SplunkHttpSourceConnectorConfig.KEYSTORE_PATH_CONF, "/tmp/testing.keystore",
        SplunkHttpSourceConnectorConfig.KEYSTORE_PASSWORD_CONF, EXPECTED_KEYSTORE_PASSWORD,
        SplunkHttpSourceConnectorConfig.SSL_RENEGOTIATION_ALLOWED_CONF, EXPECTED_SSL_RENEGOTIATION_ALLOWED.toString(),
        SplunkHttpSourceConnectorConfig.EVENT_COLLECTOR_INDEX_DEFAULT_CONF, "default",
        SplunkHttpSourceConnectorConfig.TOPIC_PREFIX_CONF, "test"
    );

    SplunkHttpSourceConnectorConfig config = new SplunkHttpSourceConnectorConfig(settings);
    SslContextFactory sslContextFactory = config.sslContextFactory();
    assertNotNull("sslContextFactory should not be null.", sslContextFactory);
    assertEquals("KeyStorePath does not match.", EXPECTED_KEYSTORE_PATH, sslContextFactory.getKeyStorePath());
    assertEquals("RenegotiationAllowed does not match.", EXPECTED_SSL_RENEGOTIATION_ALLOWED, sslContextFactory.isRenegotiationAllowed());
  }


  @Test
  public void doc() {
    System.out.println(
        MarkdownFormatter.toMarkdown(SplunkHttpSourceConnectorConfig.conf())
    );
  }
}