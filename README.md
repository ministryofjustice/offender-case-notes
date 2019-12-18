# Offender Case Notes Service

Service to provide secure access to retrieving and storing case notes about offenders

## Running localstack 
```bash
TMPDIR=/private$TMPDIR docker-compose up localstack
```

## Creating the Topic and Queue
Simpliest way is running the following script
```bash
./setup-queue.bash
```

Or you can run the scripts individually as shown below.

## Creating a topic and queue on localstack

```bash
aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events
```

Results in:
```json
{
    "TopicArn": "arn:aws:sns:eu-west-2:000000000000:offender_events"
}

```

## Creating a queue
```bash
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name keyworker_api_queue
```

Results in:
```json
{
   "QueueUrl": "http://localhost:4576/queue/offender_case_notes_queue"
}
```

## Creating a subscription
```bash
aws --endpoint-url=http://localhost:4575 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4576/queue/offender_case_notes_queue \
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
aws --endpoint-url=http://localhost:4575 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"BOOKING_NUMBER-CHANGED"}}' --message '{"eventType":"BOOKING_NUMBER-CHANGED","bookingId":1196631}' 
```

## Publish delete event message to topic
```bash
aws --endpoint-url=http://localhost:4575 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"DATA_COMPLIANCE_DELETE-OFFENDER"}}' --message '{"offenderIdDisplay":"A1234AA"}' 
```


## Read off the queue
```bash
aws --endpoint-url=http://localhost:4576 sqs receive-message --queue-url http://localhost:4576/queue/offender_case_notes_queue
```