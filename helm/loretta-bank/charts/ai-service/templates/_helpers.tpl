{{- define "ai-service.fullname" -}}
ai-service
{{- end }}

{{- define "ai-service.labels" -}}
app: ai-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "ai-service.selectorLabels" -}}
app: ai-service
{{- end }}

{{- define "ai-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
