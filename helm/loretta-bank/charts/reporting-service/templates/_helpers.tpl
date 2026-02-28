{{- define "reporting-service.fullname" -}}
reporting-service
{{- end }}

{{- define "reporting-service.labels" -}}
app: reporting-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "reporting-service.selectorLabels" -}}
app: reporting-service
{{- end }}

{{- define "reporting-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
