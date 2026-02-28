{{- define "notification-service.fullname" -}}
notification-service
{{- end }}

{{- define "notification-service.labels" -}}
app: notification-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "notification-service.selectorLabels" -}}
app: notification-service
{{- end }}

{{- define "notification-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
