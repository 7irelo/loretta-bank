variable "project" {
  description = "Project name for resource naming."
  type        = string
}

variable "environment" {
  description = "Environment name."
  type        = string
}

variable "oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider for IRSA."
  type        = string
}

variable "oidc_provider_url" {
  description = "URL of the EKS OIDC provider (without https://)."
  type        = string
}

variable "kubernetes_namespace" {
  description = "Kubernetes namespace for the External Secrets Operator service account."
  type        = string
  default     = "external-secrets"
}

variable "kubernetes_service_account" {
  description = "Kubernetes service account name for External Secrets Operator."
  type        = string
  default     = "external-secrets-sa"
}

variable "tags" {
  description = "Common tags to apply to all resources."
  type        = map(string)
  default     = {}
}
