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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class SinkRecordContentTest {


  void addRecord(Collection<SinkRecord> records, Map<String, ?> values) {
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

  @Test
  public void writeTo() throws IOException {
    Collection<SinkRecord> sinkRecords = new ArrayList<>();
    addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com"));
    addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp"));
    addRecord(sinkRecords, ImmutableMap.of("host", "hostname.example.com", "time", new Date(1472256858924L), "source", "testapp", "sourcetype", "txt", "index", "main"));

    ObjectMapper mapper = new ObjectMapper();

    SinkRecordContent content = new SinkRecordContent(mapper, sinkRecords);

    String actual;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      content.writeTo(outputStream);
      actual = new String(outputStream.toByteArray(), "UTF-8");
    }

    final String expected = "{\"host\":\"hostname.example.com\"}\n" +
        "{\"host\":\"hostname.example.com\",\"time\":1472256858.924,\"source\":\"testapp\"}\n" +
        "{\"host\":\"hostname.example.com\",\"time\":1472256858.924,\"sourcetype\":\"txt\",\"index\":\"main\",\"source\":\"testapp\"}";

    System.out.println(actual);
    Assert.assertEquals(expected, actual);
  }

}
