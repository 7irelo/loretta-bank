{{- define "frontend.fullname" -}}
loretta-frontend
{{- end }}

{{- define "frontend.labels" -}}
app: loretta-frontend
app.kubernetes.io/part-of: loretta-bank
{{- end }}

{{- define "frontend.selectorLabels" -}}
app: loretta-frontend
{{- end }}

{{- define "frontend.image" -}}
{{- if .Values.global.imageRegistry -}}
{{ .Values.global.imageRegistry }}/{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- else -}}
{{ .Values.image.repository }}:{{ .Values.image.tag }}
{{- end -}}
{{- end }}
