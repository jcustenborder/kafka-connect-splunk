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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.ImmutableSet;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ObjectMapperFactory {

  public static final ObjectMapper INSTANCE;
  static final Set<String> RESERVED_METADATA = ImmutableSet.of(
      "time",
      "host",
      "source",
      "sourcetype",
      "index"
  );

  static {
    ObjectMapper mapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addSerializer(Date.class, new DateSerializer());
    module.addDeserializer(Date.class, new DateDeserializer());
    module.addSerializer(Struct.class, new StructSerializer());
    module.addSerializer(SinkRecord.class, new SinkRecordSerializer());
    mapper.registerModule(module);
    mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    mapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    INSTANCE = mapper;
  }

  static class DateDeserializer extends JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);

      if (node.isNull()) {
        return null;
      } else if (node.isNumber()) {
        BigDecimal decimal = node.decimalValue().setScale(3);
        return new Date(decimal.unscaledValue().longValue());
      }

      return null;
    }
  }

  static class DateSerializer extends JsonSerializer<Date> {
    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
      long time = date.getTime();
      BigDecimal value = BigDecimal.valueOf(time, 3);
      jsonGenerator.writeNumber(value);
    }
  }

  static class StructSerializer extends JsonSerializer<Struct> {

    @Override
    public void serialize(Struct struct, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
      List<Field> fields = struct.schema().fields();
      Map<String, Object> values = new LinkedHashMap<>(fields.size());

      for (Field field : fields) {
        if (!RESERVED_METADATA.contains(field.name()))
          values.put(field.name(), struct.get(field));
      }

      jsonGenerator.writeObject(values);
    }
  }

  public static class Event {
    public String host;
    public Date time;
    public String sourcetype;
    public String index;
    public String source;
    public Object event;

    boolean setValue(Object key, Object value) {
      if (null == value) {
        return false;
      }

      if ("time".equals(key)) {
        if (value instanceof BigDecimal) {
          BigDecimal decimal = (BigDecimal) value;
          this.time = new Date(decimal.unscaledValue().longValue());
        } else if (value instanceof Date) {
          this.time = (Date) value;
        }
      }

      if ("host".equals(key)) {
        this.host = value.toString();
      }
      if ("source".equals(key)) {
        this.source = value.toString();
      }
      if ("sourcetype".equals(key)) {
        this.sourcetype = value.toString();
      }
      if ("index".equals(key)) {
        this.index = value.toString();
      }

      return RESERVED_METADATA.contains(key);
    }

  }

  static class SinkRecordSerializer extends JsonSerializer<SinkRecord> {

    void handleMap(Event event) {
      final Map input = (Map) event.event;
      final Map result = new LinkedHashMap(input.size());

      for (Object key : input.keySet()) {
        Object value = input.get(key);

        if (!event.setValue(key, value)) {
          result.put(key, value);
        }
      }

      event.event = result.isEmpty() ? null : result;
    }

    void handleStruct(Event event) {
      final Struct input = (Struct) event.event;
      List<Field> fields = input.schema().fields();
      final Map result = new LinkedHashMap(fields.size());

      for (Field field : fields) {
        Object key = field.name();
        Object value = input.get(field);

        if (null == value) {
          continue;
        }

        if (!event.setValue(key, value)) {
          result.put(key, value);
        }
      }

      event.event = result.isEmpty() ? null : result;
    }


    @Override
    public void serialize(SinkRecord sinkRecord, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
      Event event = new Event();
      event.event = sinkRecord.value();

      if (event.event instanceof Map) {
        handleMap(event);
      } else if (event.event instanceof Struct) {
        handleStruct(event);
      }

      //TODO: When we go to the next Kafka version. Check for null date and use the timestamp of the SinkRecord.

      jsonGenerator.writeObject(event);
    }
  }


}
