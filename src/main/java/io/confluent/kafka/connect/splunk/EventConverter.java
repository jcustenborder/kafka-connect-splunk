/**
 * Copyright © 2016 Jeremy Custenborder (jcustenborder@gmail.com)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import org.apache.kafka.common.utils.SystemTime;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.apache.kafka.connect.source.SourceRecord;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

class EventConverter {
  public static final Schema KEY_SCHEMA = SchemaBuilder.struct()
      .name("io.confluent.kafka.connect.splunk.EventKey")
      .field("host", SchemaBuilder.string().doc("The host value to assign to the event data. " +
          "This is typically the hostname of the client from which you're sending data.").build())
      .build();

  public static final Schema VALUE_SCHEMA = SchemaBuilder.struct()
      .name("io.confluent.kafka.connect.splunk.Event")
      .field("time", Timestamp.builder().optional().doc("The event time.").build())
      .field("host", SchemaBuilder.string().optional().doc("The host value to assign to the event data. " +
          "This is typically the hostname of the client from which you're sending data.").build())
      .field("source", SchemaBuilder.string().optional().doc("The source value to assign to the event data. " +
          "For example, if you're sending data from an app you're developing, you could set this key to the name " +
          "of the app.").build())
      .field("sourcetype", SchemaBuilder.string().optional().doc("The sourcetype value to assign to " +
          "the event data.").build())
      .field("index", SchemaBuilder.string().optional().doc("The name of the index by which the event data is to be " +
          "indexed. The index you specify here must within the list of allowed indexes if the token has the indexes " +
          "parameter set.").build())
      .field("event", SchemaBuilder.string().doc("This is the event it's self. This is the serialized json form. It " +
          "could be an object or a string.").optional().build())
      .build();

  private static final Map<String, ?> EMPTY_MAP = new HashMap<>();

  final SplunkHttpSourceConnectorConfig config;
  final String topicPrefix;
  final boolean topicPerIndex;
  final String topic;
  final String defaultIndex;
  final Map<String, String> indexToTopicLookup;

  Time time = new SystemTime();

  EventConverter(SplunkHttpSourceConnectorConfig config) {
    this.config = config;
    this.topicPerIndex = this.config.topicPerIndex();
    this.topicPrefix = this.config.topicPrefix();
    this.indexToTopicLookup = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
    this.topic = this.topicPerIndex ? null : this.topicPrefix;
    this.defaultIndex = this.config.defaultIndex();
  }

  static <T> void setFieldValue(JsonNode messageNode, Struct struct, String fieldName, Class<T> cls) {
    T structValue = null;

    if (messageNode.has(fieldName)) {
      JsonNode valueNode = messageNode.get(fieldName);

      if (String.class.equals(cls) && valueNode.isObject()) {
        try {
          structValue = (T) ObjectMapperFactory.INSTANCE.writeValueAsString(valueNode);
        } catch (JsonProcessingException e) {
          throw new IllegalStateException(e);
        }
      } else if (!valueNode.isNull()) {
        structValue = ObjectMapperFactory.INSTANCE.convertValue(valueNode, cls);
      }
    }

    struct.put(fieldName, structValue);
  }

  public SourceRecord convert(JsonNode messageNode, String remoteHost) {
    Preconditions.checkNotNull(messageNode, "messageNode cannot be null.");
    Preconditions.checkState(messageNode.isObject(), "messageNode must be an object.");

    Struct keyStruct = new Struct(KEY_SCHEMA);
    Struct valueStruct = new Struct(VALUE_SCHEMA);

    setFieldValue(messageNode, valueStruct, "time", Date.class);
    setFieldValue(messageNode, valueStruct, "host", String.class);
    setFieldValue(messageNode, valueStruct, "source", String.class);
    setFieldValue(messageNode, valueStruct, "sourcetype", String.class);
    setFieldValue(messageNode, valueStruct, "index", String.class);
    setFieldValue(messageNode, valueStruct, "event", String.class);

    if (null == valueStruct.get("time")) {
      valueStruct.put("time", new Date(this.time.milliseconds()));
    }

    String host = valueStruct.getString("host");

    if (null == host) {
      host = remoteHost;
      valueStruct.put("host", host);
    }

    keyStruct.put("host", valueStruct.get("host"));

    String index = valueStruct.getString("index");

    if (null == index) {
      index = this.defaultIndex;
      valueStruct.put("index", index);
    }

    String topic = this.topic;

    if (null == topic) {
      topic = this.indexToTopicLookup.get(index);

      if (null == topic) {
        topic = this.topicPrefix + index.toLowerCase();
        this.indexToTopicLookup.put(index, topic);
      }
    }

    SourceRecord sourceRecord = new SourceRecord(
        EMPTY_MAP,
        EMPTY_MAP,
        topic,
        KEY_SCHEMA,
        keyStruct,
        VALUE_SCHEMA,
        valueStruct
    );

    return sourceRecord;
  }


}
