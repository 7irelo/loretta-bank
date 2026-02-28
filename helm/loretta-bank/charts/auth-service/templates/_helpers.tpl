{{- define "auth-service.fullname" -}}
auth-service
{{- end }}

{{- define "auth-service.labels" -}}
app: auth-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "auth-service.selectorLabels" -}}
app: auth-service
{{- end }}

{{- define "auth-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
