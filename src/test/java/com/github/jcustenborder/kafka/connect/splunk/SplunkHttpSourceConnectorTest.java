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

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class SplunkHttpSourceConnectorTest {
  static final String TOPIC_PREFIX_CONF = "splunk";

  @Test
  public void test() {
    SplunkHttpSourceConnector splunkHttpSourceConnector = new SplunkHttpSourceConnector();
    Map<String, String> settings = new LinkedHashMap<>();
    settings.put(SplunkHttpSourceConnectorConfig.TOPIC_PREFIX_CONF, TOPIC_PREFIX_CONF);
    settings.put(SplunkHttpSourceConnectorConfig.KEYSTORE_PASSWORD_CONF, "password");
    settings.put(SplunkHttpSourceConnectorConfig.KEYSTORE_PATH_CONF, SplunkHttpSourceTaskTest.class.getResource("keystore.jks").toExternalForm());
    settings.put(SplunkHttpSourceConnectorConfig.EVENT_COLLECTOR_INDEX_DEFAULT_CONF, "default");
    splunkHttpSourceConnector.start(settings);
    List<Map<String, String>> taskConfigs = splunkHttpSourceConnector.taskConfigs(10);
    assertNotNull(taskConfigs);
    assertEquals(1, taskConfigs.size());
  }
}
