#!/usr/bin/env bash
aws --endpoint-url=http://localhost:4575 sns create-topic --name offender_events
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name offender_case_notes_queue
aws --endpoint-url=http://localhost:4575 sns subscribe \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events \
    --protocol sqs \
    --notification-endpoint http://localhost:4576/queue/offender_case_notes_queue \
    --attributes '{"FilterPolicy":"{\"eventType\":[\"BOOKING_NUMBER-CHANGED\"]}"}'
