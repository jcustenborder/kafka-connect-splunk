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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventConverterTest {

  final String TOPIC_PREFIX_CONF = "topic";

  SplunkHttpSourceConnectorConfig config;
  Map<String, String> settings;

  static JsonNode readNode(String fileName) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    InputStream inputStream = EventConverterTest.class.getResourceAsStream(fileName);
    return mapper.readTree(inputStream);
  }

  @BeforeEach
  public void setup() {
    this.settings = new LinkedHashMap<>();
    this.settings.put(SplunkHttpSourceConnectorConfig.TOPIC_PREFIX_CONF, TOPIC_PREFIX_CONF);
    this.settings.put(SplunkHttpSourceConnectorConfig.TOPIC_PER_INDEX_CONF, Boolean.FALSE.toString());
    this.settings.put(SplunkHttpSourceConnectorConfig.KEYSTORE_PASSWORD_CONF, "password");
    this.settings.put(SplunkHttpSourceConnectorConfig.KEYSTORE_PATH_CONF, "/tmp/foo");
    this.settings.put(SplunkHttpSourceConnectorConfig.EVENT_COLLECTOR_INDEX_DEFAULT_CONF, "default");
    this.config = new SplunkHttpSourceConnectorConfig(this.settings);
  }

  void assertSourceRecord(final Map<String, ?> expected, final ConnectRecord record, final String topic) throws JsonProcessingException {
    assertNotNull(record, "record should not be null.");
    assertNotNull(record.value(), "record.value() should not be null.");
    assertEquals(topic, record.topic(), "topic does not match.");
    assertTrue(record.key() instanceof Struct, "record.key() should be a struct");
    assertTrue(record.value() instanceof Struct, "record.value() should be a struct");

    Struct keyStruct = (Struct) record.key();
    keyStruct.validate();

    Struct valueStruct = (Struct) record.value();
    valueStruct.validate();

    for (Map.Entry<String, ?> entry : expected.entrySet()) {
      Object structValue = valueStruct.get(entry.getKey());

      if (entry.getValue() instanceof Map) {
        String text = ObjectMapperFactory.INSTANCE.writeValueAsString(entry.getValue());
        String structText = (String) structValue;
        assertEquals(text, structText, entry.getKey() + " should match.");
      } else {
        assertEquals(entry.getValue(), structValue, entry.getKey() + " should match.");
      }
    }
  }

  @Test
  public void StringBody() throws JsonProcessingException {
    final long TIME = 1472266250342L;
    Map<String, ?> expected = ImmutableMap.of(
        "time", new Date(TIME),
        "host", "localhost",
        "source", "datasource",
        "sourcetype", "txt",
        "event", "Hello world!"
    );

    JsonNode jsonNode = ObjectMapperFactory.INSTANCE.convertValue(expected, JsonNode.class);
    assertNotNull(jsonNode, "jsonNode should not be null.");
    assertTrue(jsonNode.isObject(), "jsonNode should be an object.");

    EventConverter eventConverter = new EventConverter(config);
    eventConverter.time = mock(Time.class);
    when(eventConverter.time.milliseconds()).thenReturn(TIME);
    SourceRecord sourceRecord = eventConverter.convert(jsonNode, "192.168.1.10");
    assertSourceRecord(expected, sourceRecord, TOPIC_PREFIX_CONF);
  }

  @Test
  public void complexBody() throws JsonProcessingException {
    final long TIME = 1472266250342L;
    Map<String, ?> expected = ImmutableMap.of(
        "time", new Date(TIME),
        "host", "dataserver992.example.com",
        "source", "datasource",
        "event", ImmutableMap.of(
            "message", "Something happened",
            "severity", "INFO"
        )
    );

    JsonNode jsonNode = ObjectMapperFactory.INSTANCE.convertValue(expected, JsonNode.class);
    assertNotNull(jsonNode, "jsonNode should not be null.");
    assertTrue(jsonNode.isObject(), "jsonNode should be an object.");

    EventConverter eventConverter = new EventConverter(config);
    eventConverter.time = mock(Time.class);
    when(eventConverter.time.milliseconds()).thenReturn(TIME);
    SourceRecord sourceRecord = eventConverter.convert(jsonNode, "192.168.1.10");
    assertSourceRecord(expected, sourceRecord, TOPIC_PREFIX_CONF);
  }

  @Test
  public void minimal() throws JsonProcessingException {
    final long TIME = 1472266250342L;
    Map<String, ?> expected = ImmutableMap.of(
        "time", new Date(TIME),
        "event", "Hello world!"
    );

    JsonNode jsonNode = ObjectMapperFactory.INSTANCE.convertValue(expected, JsonNode.class);
    assertNotNull(jsonNode, "jsonNode should not be null.");
    assertTrue(jsonNode.isObject(), "jsonNode should be an object.");

    EventConverter eventConverter = new EventConverter(config);
    eventConverter.time = mock(Time.class);
    when(eventConverter.time.milliseconds()).thenReturn(TIME);
    SourceRecord sourceRecord = eventConverter.convert(jsonNode, "192.168.1.10");
    assertSourceRecord(expected, sourceRecord, TOPIC_PREFIX_CONF);
  }

}
