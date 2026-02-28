{{- define "api-gateway.fullname" -}}
api-gateway
{{- end }}

{{- define "api-gateway.labels" -}}
app: api-gateway
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "api-gateway.selectorLabels" -}}
app: api-gateway
{{- end }}

{{- define "api-gateway.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
