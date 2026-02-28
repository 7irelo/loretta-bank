################################################################################
# General
################################################################################

variable "project" {
  description = "Project name used in resource naming."
  type        = string
  default     = "loretta-bank"
}

variable "environment" {
  description = "Environment name (e.g. production, staging)."
  type        = string
  default     = "production"
}

variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
  default     = "af-south-1"
}

variable "tags" {
  description = "Additional tags to apply to all resources."
  type        = map(string)
  default     = {}
}

################################################################################
# VPC
################################################################################

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

################################################################################
# EKS
################################################################################

variable "kubernetes_version" {
  description = "Kubernetes version for the EKS cluster."
  type        = string
  default     = "1.30"
}

variable "node_instance_types" {
  description = "Instance types for EKS managed node group."
  type        = list(string)
  default     = ["t4g.large"]
}

variable "node_capacity_type" {
  description = "Capacity type for EKS nodes: ON_DEMAND or SPOT."
  type        = string
  default     = "ON_DEMAND"

  validation {
    condition     = contains(["ON_DEMAND", "SPOT"], var.node_capacity_type)
    error_message = "node_capacity_type must be ON_DEMAND or SPOT."
  }
}

variable "node_min_size" {
  description = "Minimum number of EKS worker nodes."
  type        = number
  default     = 2
}

variable "node_desired_size" {
  description = "Desired number of EKS worker nodes."
  type        = number
  default     = 3
}

variable "node_max_size" {
  description = "Maximum number of EKS worker nodes."
  type        = number
  default     = 6
}

variable "node_disk_size" {
  description = "Disk size in GB for EKS worker nodes."
  type        = number
  default     = 50
}

variable "cluster_endpoint_public_access" {
  description = "Whether the EKS API endpoint is publicly accessible."
  type        = bool
  default     = true
}

variable "cluster_endpoint_public_access_cidrs" {
  description = "CIDR blocks allowed to access the EKS API endpoint."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

################################################################################
# RDS
################################################################################

variable "rds_engine_version" {
  description = "PostgreSQL engine version for RDS."
  type        = string
  default     = "16"
}

variable "rds_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.medium"
}

variable "rds_allocated_storage" {
  description = "Initial allocated storage in GB for RDS."
  type        = number
  default     = 50
}

variable "rds_max_allocated_storage" {
  description = "Maximum storage for RDS autoscaling in GB."
  type        = number
  default     = 200
}

variable "rds_database_name" {
  description = "Name of the default PostgreSQL database."
  type        = string
  default     = "lorettabank"
}

variable "rds_master_username" {
  description = "Master username for the RDS instance."
  type        = string
  default     = "dbadmin"
}

variable "rds_multi_az" {
  description = "Whether to enable multi-AZ for RDS."
  type        = bool
  default     = true
}

variable "rds_deletion_protection" {
  description = "Whether to enable deletion protection for RDS."
  type        = bool
  default     = true
}

################################################################################
# ElastiCache
################################################################################

variable "redis_engine_version" {
  description = "Redis engine version."
  type        = string
  default     = "7.1"
}

variable "redis_node_type" {
  description = "ElastiCache node type for Redis."
  type        = string
  default     = "cache.t4g.medium"
}

variable "redis_num_cache_clusters" {
  description = "Number of cache clusters in the Redis replication group."
  type        = number
  default     = 2
}

################################################################################
# MSK (Kafka)
################################################################################

variable "kafka_version" {
  description = "Apache Kafka version for MSK."
  type        = string
  default     = "3.6.0"
}

variable "msk_instance_type" {
  description = "MSK broker instance type."
  type        = string
  default     = "kafka.t3.small"
}

variable "msk_number_of_broker_nodes" {
  description = "Number of Kafka broker nodes."
  type        = number
  default     = 3
}

variable "msk_ebs_volume_size" {
  description = "EBS volume size in GB per MSK broker."
  type        = number
  default     = 100
}

################################################################################
# ECR
################################################################################

variable "ecr_max_image_count" {
  description = "Maximum number of images to retain per ECR repository."
  type        = number
  default     = 10
}

################################################################################
# ALB Controller
################################################################################

variable "alb_controller_chart_version" {
  description = "Helm chart version for the AWS Load Balancer Controller."
  type        = string
  default     = "1.7.2"
}
