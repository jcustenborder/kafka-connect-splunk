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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventConverterTest {

  static JsonNode readNode(String fileName) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    InputStream inputStream = EventConverterTest.class.getResourceAsStream(fileName);
    return mapper.readTree(inputStream);
  }

  @Test
  public void convertMinimal() throws IOException {
    JsonNode jsonNode = readNode("event.minimal.json");

    final long TIME = 1472266250342L;

    EventConverter eventConverter = new EventConverter();
    eventConverter.time = mock(Time.class);
    when(eventConverter.time.milliseconds()).thenReturn(TIME);
    SourceRecord sourceRecord = eventConverter.convert(jsonNode, "asdf");
    Assert.assertNotNull("SourceRecord should not be null.", sourceRecord);
    Assert.assertTrue("sourceRecord.value() should be a struct", sourceRecord.value() instanceof Struct);
    Struct valueStruct = (Struct) sourceRecord.value();

    Assert.assertEquals("time does not match.", new Date(TIME), valueStruct.get("time"));
  }

}
