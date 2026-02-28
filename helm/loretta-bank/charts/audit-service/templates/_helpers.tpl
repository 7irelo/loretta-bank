{{- define "audit-service.fullname" -}}
audit-service
{{- end }}

{{- define "audit-service.labels" -}}
app: audit-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "audit-service.selectorLabels" -}}
app: audit-service
{{- end }}

{{- define "audit-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
