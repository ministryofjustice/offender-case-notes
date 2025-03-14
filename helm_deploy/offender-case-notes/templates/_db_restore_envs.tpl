{{/* vim: set filetype=mustache: */}}
{{- define "db_restore.envs" -}}
{{- if or .db_restore.namespace_secrets .db_restore.env -}}
env:
{{- range $secret, $envs := .db_restore.namespace_secrets }}
  {{- range $key, $val := $envs }}
  - name: {{ $key }}
    valueFrom:
      secretKeyRef:
        key: {{ trimSuffix "?" $val }}
        name: {{ $secret }}{{ if hasSuffix "?" $val }}
        optional: true{{ end }}  {{- end }}
{{- end }}
{{- range $key, $val := .db_restore.env }}
  - name: {{ $key }}
    value: "{{ $val }}"
{{- end }}
{{- end -}}
{{- end -}}
