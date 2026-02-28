################################################################################
# General
################################################################################

output "region" {
  description = "AWS region."
  value       = var.aws_region
}

################################################################################
# VPC
################################################################################

output "vpc_id" {
  description = "VPC ID."
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs."
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs."
  value       = module.vpc.private_subnet_ids
}

################################################################################
# EKS
################################################################################

output "cluster_name" {
  description = "EKS cluster name."
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "EKS API endpoint."
  value       = module.eks.cluster_endpoint
}

output "cluster_version" {
  description = "EKS Kubernetes version."
  value       = module.eks.cluster_version
}

output "cluster_certificate_authority_data" {
  description = "Base64 encoded certificate data for the EKS cluster."
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

output "oidc_provider_arn" {
  description = "ARN of the EKS OIDC provider."
  value       = module.eks.oidc_provider_arn
}

output "configure_kubectl" {
  description = "Command to configure kubectl for this cluster."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

################################################################################
# RDS
################################################################################

output "rds_endpoint" {
  description = "RDS PostgreSQL endpoint."
  value       = module.rds.db_instance_endpoint
}

output "rds_database_name" {
  description = "RDS default database name."
  value       = module.rds.db_instance_name
}

output "rds_master_user_secret_arn" {
  description = "ARN of the Secrets Manager secret containing the RDS master password."
  value       = module.rds.db_master_user_secret_arn
}

################################################################################
# ElastiCache
################################################################################

output "redis_endpoint" {
  description = "Redis primary endpoint."
  value       = module.elasticache.redis_endpoint
}

output "redis_reader_endpoint" {
  description = "Redis reader endpoint."
  value       = module.elasticache.redis_reader_endpoint
}

################################################################################
# MSK
################################################################################

output "msk_bootstrap_brokers" {
  description = "MSK plaintext bootstrap brokers."
  value       = module.msk.bootstrap_brokers
}

output "msk_bootstrap_brokers_tls" {
  description = "MSK TLS bootstrap brokers."
  value       = module.msk.bootstrap_brokers_tls
}

################################################################################
# Secrets Manager
################################################################################

output "jwt_secret_arn" {
  description = "ARN of the JWT secret in Secrets Manager."
  value       = module.secrets_manager.jwt_secret_arn
}

output "bootstrap_credentials_secret_arn" {
  description = "ARN of the bootstrap credentials secret in Secrets Manager."
  value       = module.secrets_manager.bootstrap_credentials_secret_arn
}

output "eso_iam_role_arn" {
  description = "ARN of the IRSA role for External Secrets Operator."
  value       = module.secrets_manager.eso_iam_role_arn
}

################################################################################
# ECR
################################################################################

output "ecr_repository_urls" {
  description = "Map of ECR repository names to URLs."
  value       = module.ecr.repository_urls
}

################################################################################
# ALB Controller
################################################################################

output "alb_controller_iam_role_arn" {
  description = "ARN of the IRSA role for the ALB Controller."
  value       = module.alb_controller.alb_controller_iam_role_arn
}
