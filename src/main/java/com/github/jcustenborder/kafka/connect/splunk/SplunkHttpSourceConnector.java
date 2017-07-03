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

import com.github.jcustenborder.kafka.connect.utils.VersionUtil;
import com.github.jcustenborder.kafka.connect.utils.config.Description;
import com.github.jcustenborder.kafka.connect.utils.config.DocumentationImportant;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Description("The Splunk Source connector allows emulates a `Splunk Http Event Collector` <http://dev.splunk.com/view/event-collector/SP-CAAAE6M> to allow " +
    "application that normally log to Splunk to instead write to Kafka. The goal of this plugin is to make the change nearly " +
    "transparent to the user. This plugin currently has support for `X-Forwarded-For <https://en.wikipedia.org/wiki/X-Forwarded-For>` so " +
    "it will sit behind a load balancer nicely.")
@DocumentationImportant("This connector listens on a network port. Running more than one task or running in distributed " +
    "mode can cause some undesired effects if another task already has the port open. It is recommended that you run this " +
    "connector in :term:`Standalone Mode`.")
public class SplunkHttpSourceConnector extends SourceConnector {
  Map<String, String> settings;
  private SplunkHttpSourceConnectorConfig config;

  @Override
  public String version() {
    return VersionUtil.version(this.getClass());
  }

  @Override
  public void start(Map<String, String> map) {
    this.config = new SplunkHttpSourceConnectorConfig(map);
    this.settings = map;
  }

  @Override
  public Class<? extends Task> taskClass() {
    return SplunkHttpSourceTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int i) {
    List<Map<String, String>> configs = new ArrayList<>();
    configs.add(this.settings);
    return configs;
  }

  @Override
  public void stop() {
  }

  @Override
  public ConfigDef config() {
    return SplunkHttpSourceConnectorConfig.conf();
  }
}
