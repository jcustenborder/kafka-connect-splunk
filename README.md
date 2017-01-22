This connector allows Kafka Connect to emulate a [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).
This connector support receiving data and writing data to Splunk.

# Source Connector

The Splunk Source connector allows emulates a [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M) to allow
application that normally log to Splunk to instead write to Kafka. The goal of this plugin is to make the change nearly
transparent to the user. This plugin currently has support for [X-Forwarded-For](https://en.wikipedia.org/wiki/X-Forwarded-For) so
it will sit behind a load balancer nicely. 

## Configuration

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

### Example Config

```
name=splunk-http-source
tasks.max=1
connector.class=com.github.jcustenborder.kafka.connect.splunk.SplunkHttpSourceConnector
splunk.ssl.key.store.path=/etc/security/keystore.jks
splunk.ssl.key.store.password=password
splunk.collector.index.default=main
```

# Sink Connector

The Sink Connector will transform data from a Kafka topic into a batch of json messages that will be written via HTTP to
a configured [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M). 

## Configuration

| Name                            | Description                                                                             | Type     | Default  | Valid Values | Importance |
|---------------------------------|-----------------------------------------------------------------------------------------|----------|----------|--------------|------------|
| splunk.auth.token               | The authorization token to use when writing data to splunk.                             | password |          |              | high       |
| splunk.remote.host              | The hostname of the remote splunk host to write data do.                                | string   |          |              | high       |
| splunk.ssl.enabled              | Flag to determine if the connection to splunk should be over ssl.                       | boolean  | true     |              | high       |
| splunk.ssl.trust.store.password | Password for the trust store.                                                           | password | [hidden] |              | high       |
| splunk.ssl.trust.store.path     | Path on the local disk to the certificate trust store.                                  | string   | ""       |              | high       |
| splunk.remote.port              | Port on the remote splunk server to write to.                                           | int      | 8088     |              | medium     |
| splunk.ssl.validate.certs       | Flag to determine if ssl connections should validate the certificateof the remote host. | boolean  | true     |              | medium     |

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