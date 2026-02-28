variable "project_name" {
  description = "Project name used in resource naming."
  type        = string
  default     = "loretta-bank"
}

variable "environment" {
  description = "Environment name (for example: dev, stage, prod)."
  type        = string
  default     = "dev"
}

variable "aws_region" {
  description = "AWS region for the EKS cluster."
  type        = string
  default     = "us-east-1"
}

variable "kubernetes_version" {
  description = "Kubernetes control plane version for EKS."
  type        = string
  default     = "1.30"
}

variable "vpc_cidr" {
  description = "VPC CIDR for the cluster network."
  type        = string
  default     = "10.42.0.0/16"
}

variable "az_count" {
  description = "Number of AZs/subnets to create (EKS requires at least 2 AZs)."
  type        = number
  default     = 2

  validation {
    condition     = var.az_count >= 2
    error_message = "az_count must be at least 2 for EKS."
  }
}

variable "node_instance_types" {
  description = "Worker node instance types. Default uses Graviton for lower cost."
  type        = list(string)
  default     = ["t4g.medium"]
}

variable "node_capacity_type" {
  description = "Node capacity type: SPOT or ON_DEMAND."
  type        = string
  default     = "SPOT"

  validation {
    condition     = contains(["SPOT", "ON_DEMAND"], var.node_capacity_type)
    error_message = "node_capacity_type must be SPOT or ON_DEMAND."
  }
}

variable "node_min_size" {
  description = "Minimum number of EKS worker nodes."
  type        = number
  default     = 1
}

variable "node_desired_size" {
  description = "Desired number of EKS worker nodes."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum number of EKS worker nodes."
  type        = number
  default     = 2
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "CIDR ranges allowed to reach the EKS API endpoint. Restrict this for better security."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "tags" {
  description = "Additional tags to apply to all resources."
  type        = map(string)
  default     = {}
}
