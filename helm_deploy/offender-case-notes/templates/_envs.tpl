    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: ELITE2_API_BASE_URL
    value: "{{ .Values.env.ELITE2_API_BASE_URL }}"

  - name: OAUTH_API_BASE_URL
    value: "{{ .Values.env.OAUTH_API_BASE_URL }}"

  - name: TOKENVERIFICATION_API_BASE_URL
    value: "{{ .Values.env.TOKENVERIFICATION_API_BASE_URL }}"

  - name: TOKENVERIFICATION_ENABLED
    value: "{{ .Values.env.TOKENVERIFICATION_ENABLED }}"

  - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
    value: "{{ .Values.env.SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI }}"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        key: APPINSIGHTS_INSTRUMENTATIONKEY
        name: {{ template "app.name" . }}
  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: APPLICATIONINSIGHTS_CONFIGURATION_FILE
    value: "{{ .Values.env.APPLICATIONINSIGHTS_CONFIGURATION_FILE }}"

  - name: DATABASE_USERNAME
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_username

  - name: DATABASE_PASSWORD
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_password

  - name: DATABASE_NAME
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: database_name

  - name: DATABASE_ENDPOINT
    valueFrom:
      secretKeyRef:
        name: dps-rds-instance-output
        key: rds_instance_endpoint

  - name: SNS_PROVIDER
    value: aws

  - name: HMPPS_SQS_TOPICS_OFFENDEREVENTS_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: access_key_id

  - name: HMPPS_SQS_TOPICS_OFFENDEREVENTS_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: secret_access_key

  - name: HMPPS_SQS_TOPICS_OFFENDEREVENTS_ARN
    valueFrom:
      secretKeyRef:
        name: offender-events-topic
        key: topic_arn

  - name: HMPPS_SQS_QUEUES_EVENT_QUEUE_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: ocn-events-sqs-instance-output
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_EVENT_QUEUE_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: ocn-events-sqs-instance-output
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_EVENT_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: ocn-events-sqs-instance-output
        key: sqs_ocne_name

  - name: HMPPS_SQS_QUEUES_EVENT_DLQ_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: ocn-events-sqs-dl-instance-output
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_EVENT_DLQ_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: ocn-events-sqs-dl-instance-output
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_EVENT_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: ocn-events-sqs-dl-instance-output
        key: sqs_ocne_name

  - name: OFFENDER_CASE_NOTES_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: OFFENDER_CASE_NOTES_CLIENT_ID

  - name: OFFENDER_CASE_NOTES_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: OFFENDER_CASE_NOTES_CLIENT_SECRET
{{- end -}}
