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
package io.confluent.kafka.connect.splunk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.Assert.*;

public class DateSerializationTest {
  ObjectMapper mapper;

  @Before
  public void setup() {
    this.mapper = ObjectMapperFactory.create();
  }

  @Test
  public void roundTrip() {
    final Date expected = new Date(1472507332123L);
    final BigDecimal expectedIntermediateValue = BigDecimal.valueOf(expected.getTime(), 3);
    JsonNode intermediateNode = this.mapper.convertValue(expected, JsonNode.class);
    assertNotNull(intermediateNode);
    assertTrue("intermediateNode should be a number.", intermediateNode.isNumber());
    BigDecimal intermediateValue = intermediateNode.decimalValue();
    intermediateValue.setScale(3);
    assertEquals(expectedIntermediateValue, intermediateValue);
    final Date actual = this.mapper.convertValue(intermediateNode, Date.class);
    assertEquals(expected, actual);
  }

  @Test
  public void nulls() {
    Date date = this.mapper.convertValue(null, Date.class);
    assertNull(date);
    JsonNode inputNode = this.mapper.convertValue(null, JsonNode.class);
    date = this.mapper.convertValue(inputNode, Date.class);
    assertNull(date);
  }

}
