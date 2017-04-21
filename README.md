This connector allows Kafka Connect to emulate a [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).
This connector support receiving data and writing data to Splunk.

# Configuration

## SplunkHttpSinkConnector

The Sink Connector will transform data from a Kafka topic into a batch of json messages that will be written via HTTP to a configured [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).

```properties
name=connector1
tasks.max=1
connector.class=com.github.jcustenborder.kafka.connect.splunk.SplunkHttpSinkConnector

# Set these required values
splunk.remote.host=
splunk.auth.token=
```

| Name                            | Description                                                                                                                             | Type     | Default  | Valid Values | Importance |
|---------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|----------|----------|--------------|------------|
| splunk.auth.token               | The authorization token to use when writing data to splunk.                                                                             | password |          |              | high       |
| splunk.remote.host              | The hostname of the remote splunk host to write data do.                                                                                | string   |          |              | high       |
| splunk.ssl.enabled              | Flag to determine if the connection to splunk should be over ssl.                                                                       | boolean  | true     |              | high       |
| splunk.ssl.trust.store.password | Password for the trust store.                                                                                                           | password | [hidden] |              | high       |
| splunk.ssl.trust.store.path     | Path on the local disk to the certificate trust store.                                                                                  | string   | ""       |              | high       |
| splunk.remote.port              | Port on the remote splunk server to write to.                                                                                           | int      | 8088     |              | medium     |
| splunk.ssl.validate.certs       | Flag to determine if ssl connections should validate the certificateof the remote host.                                                 | boolean  | true     |              | medium     |
| splunk.connect.timeout.ms       | The maximum amount of time for a connection to be established.                                                                          | int      | 20000    |              | low        |
| splunk.curl.logging.enabled     | Flag to determine if requests to Splunk should be logged in curl form. This will output a curl command to replicate the call to Splunk. | boolean  | false    |              | low        |
| splunk.read.timeout.ms          | Sets the timeout in milliseconds to read data from an established connection or 0 for an infinite timeout.                              | int      | 30000    |              | low        |

## SplunkHttpSourceConnector

The Splunk Source connector allows emulates a [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M) to allow application that normally log to Splunk to instead write to Kafka. The goal of this plugin is to make the change nearly transparent to the user. This plugin currently has support for [X-Forwarded-For](https://en.wikipedia.org/wiki/X-Forwarded-For) so it will sit behind a load balancer nicely.

```properties
name=connector1
tasks.max=1
connector.class=com.github.jcustenborder.kafka.connect.splunk.SplunkHttpSourceConnector

# Set these required values
splunk.ssl.key.store.password=
splunk.collector.index.default=
splunk.ssl.key.store.path=
kafka.topic=
```

| Name                             | Description                                                                                                                                                                                                                                                                                                 | Type     | Default                   | Valid Values | Importance |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|---------------------------|--------------|------------|
| kafka.topic                      | This value contains the topic that the messages will be written to. If topic per index is enabled this will be the prefix for the topic. If not this will be the exact topic.                                                                                                                               | string   |                           |              | high       |
| splunk.collector.index.default   | The index that will be used if no index is specified in the event message.                                                                                                                                                                                                                                  | string   |                           |              | high       |
| splunk.ssl.key.store.password    | The password for opening the keystore.                                                                                                                                                                                                                                                                      | password |                           |              | high       |
| splunk.ssl.key.store.path        | The path to the keystore on the local filesystem.                                                                                                                                                                                                                                                           | string   |                           |              | high       |
| splunk.port                      | The port to configure the http listener on.                                                                                                                                                                                                                                                                 | int      | 8088                      |              | high       |
| topic.per.index                  | Flag determines if the all generated messages should be written toa single topic or should the messages be placed in a topic prefixed by the supplied index. If true the `kafka.topic` setting will be concatenated along with the index name. If false the `kafka.topic` value will be used for the topic. | boolean  | false                     |              | medium     |
| backoff.ms                       | The number of milliseconds to back off when there are no records in thequeue.                                                                                                                                                                                                                               | int      | 100                       |              | low        |
| batch.size                       | Maximum number of records to write per poll call.                                                                                                                                                                                                                                                           | int      | 10000                     |              | low        |
| splunk.collector.index.allowed   | The indexes this connector allows data to be written for. Specifying an index outside of this list will result in an exception being raised.                                                                                                                                                                | list     | []                        |              | low        |
| splunk.collector.url             | Path fragement the servlet should respond on                                                                                                                                                                                                                                                                | string   | /services/collector/event |              | low        |
| splunk.ssl.renegotiation.allowed | Flag to determine if ssl renegotiation is allowed.                                                                                                                                                                                                                                                          | boolean  | true                      |              | low        |


# Schemas

## com.github.jcustenborder.kafka.connect.splunk.EventKey

This schema represents the key for the data received from the Splunk listener.

| Name | Optional | Schema                                                                                                | Default Value | Documentation                                                                                                            |
|------|----------|-------------------------------------------------------------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------|
| host | false    | [String](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Schema.Type.html#STRING) |               | The host value to assign to the event data. This is typically the hostname of the client from which you're sending data. |

## com.github.jcustenborder.kafka.connect.splunk.Event

This schema represents the data received from the Splunk listener.

| Name       | Optional | Schema                                                                                                | Default Value | Documentation                                                                                                                                                                  |
|------------|----------|-------------------------------------------------------------------------------------------------------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| time       | true     | [Timestamp](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Timestamp.html)       |               | The event time.                                                                                                                                                                |
| host       | true     | [String](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Schema.Type.html#STRING) |               | The host value to assign to the event data. This is typically the hostname of the client from which you're sending data.                                                       |
| source     | true     | [String](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Schema.Type.html#STRING) |               | The source value to assign to the event data. For example, if you're sending data from an app you're developing, you could set this key to the name of the app.                |
| sourcetype | true     | [String](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Schema.Type.html#STRING) |               | The sourcetype value to assign to the event data.                                                                                                                              |
| index      | true     | [String](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Schema.Type.html#STRING) |               | The name of the index by which the event data is to be indexed. The index you specify here must within the list of allowed indexes if the token has the indexes parameter set. |
| event      | true     | [String](https://kafka.apache.org/0102/javadoc/org/apache/kafka/connect/data/Schema.Type.html#STRING) |               | This is the event it's self. This is the serialized json form. It could be an object or a string.                                                                              |


### Example Config

This configuration will write to Splunk over SSL but will not verify the certificate.

```
name=splunk-http-sink
topics=syslog-udp
tasks.max=1
connector.class=com.github.jcustenborder.kafka.connect.splunk.SplunkHttpSinkConnector
splunk.remote.host=192.168.99.100
splunk.remote.port=8088
splunk.ssl.enabled=true
splunk.ssl.validate.certs=false
splunk.auth.token=**********
```

## Writing data to Splunk.

The Sink Connector uses the Splunk Http Event Collector as it's target to write data to Splunk. To use
this plugin you will need to [configure an endpoint](http://docs.splunk.com/Documentation/Splunk/6.4.3/Data/UsetheHTTPEventCollector).

The Sink Connector will pull over all of the fields that are in the incoming schema. If there is a timestamp field named
`date` or `time` it will be converted to a Splunk timestamp and moved to the `time` field. The `host` 
or `hostname` if it exists will be placed in the `host` field. All other fields will be copied to the `event` 
object.

Here is an example of an event generated by [Kafka Connect Syslog](https://github.com/jcustenborder/kafka-connect-syslog) written to Splunk.

```json
{
  "host": "vpn.example.com",
  "time": 1472342182,
  "event": {
    "charset": "UTF-8",
    "level": "6",
    "remote_address": "\/10.10.0.1:514",
    "message": "filterlog: 9,16777216,,1000000103,igb2,match,block,in,4,0x0,,64,5581,0,none,6,tcp,40,10.10.1.22,72.21.194.87,55450,443,0,A,,2551909476,8192,,",
    "facility": "16"
  }
}
```

# Running in development

## Run the connector

```bash
./bin/debug.sh
```

## Suspend waiting on the debugger to attach.

```bash
export SUSPEND='y'
./bin/debug.sh
```