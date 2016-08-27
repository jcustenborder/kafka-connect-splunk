This connector allows Kafka Connect to emulate a [Splunk Http Event Collector](http://dev.splunk.com/view/event-collector/SP-CAAAE6M).
This connector support receiving data and writing data to Splunk.

# Source Connector

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

# Running in development

```
mvn clean package
export CLASSPATH="$(find target/ -type f -name '*.jar'| grep '\-package' | tr '\n' ':')"
$CONFLUENT_HOME/bin/connect-standalone $CONFLUENT_HOME/etc/schema-registry/connect-avro-standalone.properties config/MySourceConnector.properties
```
