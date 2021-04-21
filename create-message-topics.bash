#!/usr/bin/env bash
aws --endpoint-url=http://localhost:4566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"BOOKING_NUMBER-CHANGED"}}' --message '{"eventType":"BOOKING_NUMBER-CHANGED","bookingId":1196631}'
aws --endpoint-url=http://localhost:4566 sns publish --topic-arn arn:aws:sns:eu-west-2:000000000000:offender_events --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"DATA_COMPLIANCE_DELETE-OFFENDER"}}' --message '{"offenderIdDisplay":"A1234AA"}'
