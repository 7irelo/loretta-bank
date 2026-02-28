{{- define "transaction-service.fullname" -}}
transaction-service
{{- end }}

{{- define "transaction-service.labels" -}}
app: transaction-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "transaction-service.selectorLabels" -}}
app: transaction-service
{{- end }}

{{- define "transaction-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
