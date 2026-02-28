{{- define "discovery-service.fullname" -}}
discovery-service
{{- end }}

{{- define "discovery-service.labels" -}}
app: discovery-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "discovery-service.selectorLabels" -}}
app: discovery-service
{{- end }}

{{- define "discovery-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
