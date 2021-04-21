# Offender Case Notes Service

Service to provide secure access to retrieving and storing case notes about offenders

### Running against localstack

Localstack has been introduced for some integration tests and it is also possible to run the application against localstack.

* In the root of the localstack project, run command
```
sudo rm -rf /tmp/localstack && docker-compose -f docker-compose-localstack.yaml down && docker-compose -f docker-compose-localstack.yaml up
```
to clear down and then bring up localstack
* Start the Spring Boot app with profile='localstack'
* You can now use the aws CLI to send messages to the queue
* The queue's health status should appear at the local healthcheck: http://localhost:8083/health
* Note that you will also need local copies of Oauth server, Case notes API and Delius API running to do anything useful

### Running the tests

With localstack now up and running (see previous section), run
```bash
./gradlew test
```
```

## Creating the Topic and Queue
Simpliest way is running the following script
```bash
./setup-queue.bash
```

Or you can run the scripts individually as shown below.

## Creating a topic and queue on localstack

```bash
aws --endpoint-url=http://localhost:4566 sns create-topic --name offender_events
```

Results in:
```json
{
    "TopicArn": "arn:aws:sns:eu-west-2:000000000000:offender_events"
}

```

## Creating a queue
```bash
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name keyworker_api_queue
```

Results in:
```json
{
   "QueueUrl": "http://localhost:4566/queue/offender_case_notes_queue"
}
```

## Creating a subscription
```bash
aws --endpoint-url=http://localhost:4566 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4566/queue/offender_case_notes_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"BOOKING_NUMBER-CHANGED\"]}"}'
```

Results in:
```json
{
    "SubscriptionArn": "arn:aws:sns:eu-west-2:000000000000:offender_events:618f126c-ab2f-4c72-874d-05ac1a3c3e95"
}
```

## Publish merge event message to topic
```bash
aws --endpoint-url=http://localhost:4566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"BOOKING_NUMBER-CHANGED"}}' --message '{"eventType":"BOOKING_NUMBER-CHANGED","bookingId":1196631}' 
```

## Publish delete event message to topic
```bash
aws --endpoint-url=http://localhost:4566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"DATA_COMPLIANCE_DELETE-OFFENDER"}}' --message '{"offenderIdDisplay":"A1234AA"}' 
```


## Read off the queue
```bash
aws --endpoint-url=http://localhost:4566 sqs receive-message --queue-url http://localhost:4566/queue/offender_case_notes_queue
```
