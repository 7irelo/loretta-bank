locals {
  cluster_name = "${var.project}-${var.environment}"

  azs = slice(data.aws_availability_zones.available.names, 0, 3)

  ecr_repositories = [
    "discovery-service",
    "api-gateway",
    "auth-service",
    "customer-service",
    "account-service",
    "transaction-service",
    "notification-service",
    "audit-service",
    "reporting-service",
    "frontend",
  ]

  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "terraform"
    },
    var.tags
  )
}
