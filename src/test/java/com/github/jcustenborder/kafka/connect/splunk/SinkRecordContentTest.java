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

import com.google.common.collect.ImmutableMap;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class SinkRecordContentTest {
  private static final Logger log = LoggerFactory.getLogger(SinkRecordContentTest.class);

  public static void addRecord(Collection<SinkRecord> records, Map<String, ?> values) {
    Struct valueStruct = new Struct(EventConverter.VALUE_SCHEMA);
    for (Map.Entry<String, ?> entry : values.entrySet()) {
      valueStruct.put(entry.getKey(), entry.getValue());
    }

    records.add(
        new SinkRecord(
            "topic",
            1,
            EventConverter.KEY_SCHEMA,
            null,
            EventConverter.VALUE_SCHEMA,
            valueStruct,
            1L
        )
    );
  }

  SinkRecord record(Object value) {
    return record(null, value);
  }

  SinkRecord record(Schema schema, Object value) {
    SinkRecord record = new SinkRecord(
        "topic",
        1,
        null,
        null,
        schema,
        value,
        1L
    );

    return record;
  }

  void test(final SinkRecord input, final String expected) throws IOException {
    SinkRecordContent content = new SinkRecordContent(input);
    final String actual;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      content.writeTo(outputStream);
      actual = new String(outputStream.toByteArray(), "UTF-8");
    }
    log.trace("actual = {}", actual);
    assertEquals(expected, actual);
  }

  @Test
  public void map() throws IOException {
    final Map<String, ?> value = ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main");
    final SinkRecord record = record(value);
    final String expected = "{\"host\":\"hostname.example.com\",\"time\":1472256858.924,\"sourcetype\":\"txt\",\"index\":\"main\",\"source\":\"testapp\"}";
    test(record, expected);
  }

  @Test
  public void struct000() throws IOException {
    final Struct value = new Struct(EventConverter.VALUE_SCHEMA).put("host", "hostname.example.com");
    final SinkRecord record = record(value);

    final String expected = "{\"host\":\"hostname.example.com\"}";
    test(record, expected);
  }

  @Test
  public void struct001() throws IOException {
    final Struct value = new Struct(EventConverter.VALUE_SCHEMA)
        .put("host", "hostname.example.com")
        .put("time", new Date(1472256858924L))
        .put("source", "testapp");
    final SinkRecord record = record(value);

    final String expected = "{\"host\":\"hostname.example.com\",\"time\":1472256858.924,\"source\":\"testapp\"}";
    test(record, expected);
  }

  @Test
  public void struct002() throws IOException {
    final Struct value = new Struct(EventConverter.VALUE_SCHEMA)
        .put("host", "hostname.example.com")
        .put("time", new Date(1472256858924L))
        .put("source", "testapp")
        .put("sourcetype", "txt")
        .put("index", "main");
    final SinkRecord record = record(value);

    final String expected = "{\"host\":\"hostname.example.com\",\"time\":1472256858.924,\"sourcetype\":\"txt\",\"index\":\"main\",\"source\":\"testapp\"}";
    test(record, expected);
  }

  @Test
  public void string001() throws IOException {
    final SinkRecord record = record("This is a random value");

    final String expected = "{\"event\":\"This is a random value\"}";
    test(record, expected);
  }

  @Test
  public void boolean001() throws IOException {
    final SinkRecord record = record(true);

    final String expected = "{\"event\":true}";
    test(record, expected);
  }

  @Test
  public void number001() throws IOException {
    final SinkRecord record = record(12341233);

    final String expected = "{\"event\":12341233}";
    test(record, expected);
  }
}
