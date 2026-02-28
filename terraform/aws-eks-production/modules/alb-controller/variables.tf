variable "project" {
  description = "Project name for resource naming."
  type        = string
}

variable "environment" {
  description = "Environment name."
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster."
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

variable "vpc_id" {
  description = "VPC ID for the ALB Controller."
  type        = string
}

variable "aws_region" {
  description = "AWS region for the ALB Controller."
  type        = string
}

variable "chart_version" {
  description = "Helm chart version for the AWS Load Balancer Controller."
  type        = string
  default     = "1.7.2"
}

variable "namespace" {
  description = "Kubernetes namespace for the ALB Controller."
  type        = string
  default     = "kube-system"
}

variable "tags" {
  description = "Common tags to apply to all resources."
  type        = map(string)
  default     = {}
}
