{{- define "customer-service.fullname" -}}
customer-service
{{- end }}

{{- define "customer-service.labels" -}}
app: customer-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "customer-service.selectorLabels" -}}
app: customer-service
{{- end }}

{{- define "customer-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
