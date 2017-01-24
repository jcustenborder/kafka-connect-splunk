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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

class EventIterator implements Iterator<JsonNode>, AutoCloseable {
  final JsonFactory jsonFactory;
  final JsonParser jsonParser;
  final Iterator<JsonNode> iterator;

  EventIterator(JsonFactory jsonFactory, JsonParser jsonParser, Iterator<JsonNode> iterator) {
    this.jsonFactory = jsonFactory;
    this.jsonParser = jsonParser;
    this.iterator = iterator;
  }

  public static EventIterator create(JsonFactory jsonFactory, BufferedReader bufferedReader) throws IOException {
    JsonParser jsonParser = jsonFactory.createParser(bufferedReader);
    return create(jsonFactory, jsonParser);
  }

  public static EventIterator create(JsonFactory jsonFactory, InputStream inputStream) throws IOException {
    JsonParser jsonParser = jsonFactory.createParser(inputStream);
    return create(jsonFactory, jsonParser);
  }

  public static EventIterator create(JsonFactory jsonFactory, JsonParser jsonParser) throws IOException {
    Iterator<JsonNode> iterator = ObjectMapperFactory.INSTANCE.readValues(jsonParser, JsonNode.class);
    return new EventIterator(jsonFactory, jsonParser, iterator);
  }

  @Override
  public boolean hasNext() {
    return this.iterator.hasNext();
  }

  @Override
  public JsonNode next() {
    return this.iterator.next();
  }

  @Override
  public void remove() {

  }

  @Override
  public void close() throws Exception {
    this.jsonParser.close();
  }
}
