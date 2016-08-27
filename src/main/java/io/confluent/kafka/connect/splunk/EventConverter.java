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

class EventConverter {
  public static final Schema KEY_SCHEMA = SchemaBuilder.struct()
      .name("io.confluent.kafka.connect.splunk.EventKey")
      .field("host", SchemaBuilder.string().doc("The host value to assign to the event data. " +
          "This is typically the hostname of the client from which you're sending data.").build())
      .build();
  public static final Schema VALUE_SCHEMA = SchemaBuilder.struct()
      .name("io.confluent.kafka.connect.splunk.Event")
      .field("time", Timestamp.builder().doc("The event time.").build())
      .field("host", SchemaBuilder.string().doc("The host value to assign to the event data. " +
          "This is typically the hostname of the client from which you're sending data.").build())
      .field("source", SchemaBuilder.string().optional().doc("The source value to assign to the event data. " +
          "For example, if you're sending data from an app you're developing, you could set this key to the name " +
          "of the app.").build())
      .field("sourcetype", SchemaBuilder.string().optional().doc("The sourcetype value to assign to " +
          "the event data.").build())
      .field("index", SchemaBuilder.string().optional().doc("The name of the index by which the event data is to be " +
          "indexed. The index you specify here must within the list of allowed indexes if the token has the indexes " +
          "parameter set.").build())
      .build();
  static final Map<String, ?> EMPTY_MAP = new HashMap<>();
  Time time = new SystemTime();

  public SourceRecord convert(JsonNode jsonNode, String topic) {
    Preconditions.checkNotNull(jsonNode, "jsonNode cannot be null.");
    Preconditions.checkState(jsonNode.isObject(), "jsonNode must be an object.");

    Struct keyStruct = new Struct(KEY_SCHEMA);
    Struct valueStruct = new Struct(VALUE_SCHEMA);

    if (jsonNode.has("time")) {
      JsonNode foo = jsonNode.get("time");
    } else {
      valueStruct.put("time", new Date(time.milliseconds()));
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
