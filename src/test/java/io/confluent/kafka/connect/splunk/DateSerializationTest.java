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
