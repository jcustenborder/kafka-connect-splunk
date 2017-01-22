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
package com.github.jcustenborder.kafka.connect.splunk;

import com.google.api.client.http.HttpContent;
import org.apache.kafka.connect.sink.SinkRecord;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;

class SinkRecordContent implements HttpContent {
  final Collection<SinkRecord> sinkRecords;

  SinkRecordContent(SinkRecord... records) {
    this(Arrays.asList(records));
  }

  SinkRecordContent(Collection<SinkRecord> sinkRecords) {
    this.sinkRecords = sinkRecords;
  }

  @Override
  public long getLength() throws IOException {
    return -1;
  }

  @Override
  public String getType() {
    return null;
  }

  @Override
  public boolean retrySupported() {
    return false;
  }


  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    int index = 0;

    for (SinkRecord sinkRecord : this.sinkRecords) {
      if (index > 0) {
        outputStream.write((int) '\n');
      }

      ObjectMapperFactory.INSTANCE.writeValue(outputStream, sinkRecord);
      index++;
    }

    outputStream.flush();
  }

}
