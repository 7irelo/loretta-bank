{{- define "account-service.fullname" -}}
account-service
{{- end }}

{{- define "account-service.labels" -}}
app: account-service
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "account-service.selectorLabels" -}}
app: account-service
{{- end }}

{{- define "account-service.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
