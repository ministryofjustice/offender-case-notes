    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: JWT_PUBLIC_KEY
    value: "{{ .Values.env.JWT_PUBLIC_KEY }}"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

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
{{- end -}}
