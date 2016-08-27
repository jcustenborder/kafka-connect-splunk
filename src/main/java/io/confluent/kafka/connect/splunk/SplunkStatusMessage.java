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

import com.google.api.client.util.Key;

public class SplunkStatusMessage {
  @Key("text")
  String text;

  @Key("code")
  Integer code;

  public String text() {
    return this.text;
  }

  public void text(String text) {
    this.text = text;
  }

  public Integer code() {
    return this.code;
  }

  public void code(Integer code) {
    this.code = code;
  }

  public boolean isSuccessful() {
    return 0 == this.code;
  }
}
