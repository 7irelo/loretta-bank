variable "project" {
  description = "Project name for resource naming."
  type        = string
}

variable "environment" {
  description = "Environment name."
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS cluster."
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the cluster will be deployed."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs for EKS worker nodes."
  type        = list(string)
}

variable "control_plane_subnet_ids" {
  description = "Subnet IDs for the EKS control plane ENIs."
  type        = list(string)
}

variable "node_instance_types" {
  description = "Instance types for the managed node group."
  type        = list(string)
  default     = ["t4g.large"]
}

variable "node_capacity_type" {
  description = "Capacity type: ON_DEMAND or SPOT."
  type        = string
  default     = "ON_DEMAND"
}

variable "node_min_size" {
  description = "Minimum number of worker nodes."
  type        = number
  default     = 2
}

variable "node_max_size" {
  description = "Maximum number of worker nodes."
  type        = number
  default     = 6
}

variable "node_desired_size" {
  description = "Desired number of worker nodes."
  type        = number
  default     = 3
}

variable "node_disk_size" {
  description = "Disk size in GB for worker nodes."
  type        = number
  default     = 50
}

variable "cluster_endpoint_public_access" {
  description = "Whether the EKS API endpoint is publicly accessible. Prefer false and reach it via VPN or a bastion."
  type        = bool
  default     = false
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "CIDR blocks allowed to reach a public EKS API endpoint (office or VPN egress IPs)."
  type        = list(string)
  default     = []

  validation {
    condition     = !contains(var.cluster_endpoint_public_access_cidrs, "0.0.0.0/0")
    error_message = "Do not expose the EKS API to the whole internet; list specific CIDRs."
  }
}

variable "tags" {
  description = "Common tags to apply to all resources."
  type        = map(string)
  default     = {}
}
