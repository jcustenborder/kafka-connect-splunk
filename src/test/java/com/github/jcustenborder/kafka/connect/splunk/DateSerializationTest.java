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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DateSerializationTest {
  void roundtrip(final Date expected) throws IOException {
    String input = ObjectMapperFactory.INSTANCE.writeValueAsString(expected);
    Date actual = ObjectMapperFactory.INSTANCE.readValue(input, Date.class);
    assertEquals(expected, actual, "actual does not match.");
  }

  @Test
  public void roundtrip() throws IOException {
    final Date expected = new Date(1472507332123L);
    roundtrip(expected);
    roundtrip(null);
  }
}
