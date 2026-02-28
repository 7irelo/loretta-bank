################################################################################
# VPC
################################################################################

module "vpc" {
  source = "./modules/vpc"

  project            = var.project
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = local.azs
  tags               = local.common_tags
}

################################################################################
# EKS Cluster
################################################################################

module "eks" {
  source = "./modules/eks"

  project     = var.project
  environment = var.environment

  cluster_version              = var.kubernetes_version
  vpc_id                       = module.vpc.vpc_id
  private_subnet_ids           = module.vpc.private_subnet_ids
  control_plane_subnet_ids     = concat(module.vpc.public_subnet_ids, module.vpc.private_subnet_ids)

  node_instance_types              = var.node_instance_types
  node_capacity_type               = var.node_capacity_type
  node_min_size                    = var.node_min_size
  node_max_size                    = var.node_max_size
  node_desired_size                = var.node_desired_size
  node_disk_size                   = var.node_disk_size
  cluster_endpoint_public_access       = var.cluster_endpoint_public_access
  cluster_endpoint_public_access_cidrs = var.cluster_endpoint_public_access_cidrs

  tags = local.common_tags
}

################################################################################
# RDS (PostgreSQL)
################################################################################

module "rds" {
  source = "./modules/rds"

  project     = var.project
  environment = var.environment

  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_security_group_id]

  engine_version        = var.rds_engine_version
  instance_class        = var.rds_instance_class
  allocated_storage     = var.rds_allocated_storage
  max_allocated_storage = var.rds_max_allocated_storage
  database_name         = var.rds_database_name
  master_username       = var.rds_master_username
  multi_az              = var.rds_multi_az
  deletion_protection   = var.rds_deletion_protection

  tags = local.common_tags
}

################################################################################
# ElastiCache (Redis)
################################################################################

module "elasticache" {
  source = "./modules/elasticache"

  project     = var.project
  environment = var.environment

  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_security_group_id]

  engine_version             = var.redis_engine_version
  node_type                  = var.redis_node_type
  num_cache_clusters         = var.redis_num_cache_clusters
  automatic_failover_enabled = true

  tags = local.common_tags
}

################################################################################
# MSK (Kafka)
################################################################################

module "msk" {
  source = "./modules/msk"

  project     = var.project
  environment = var.environment

  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_group_ids = [module.eks.node_security_group_id]

  kafka_version          = var.kafka_version
  instance_type          = var.msk_instance_type
  number_of_broker_nodes = var.msk_number_of_broker_nodes
  ebs_volume_size        = var.msk_ebs_volume_size

  tags = local.common_tags
}

################################################################################
# Secrets Manager
################################################################################

module "secrets_manager" {
  source = "./modules/secrets-manager"

  project     = var.project
  environment = var.environment

  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_provider_url = module.eks.oidc_provider_url

  tags = local.common_tags
}

################################################################################
# ECR Repositories
################################################################################

module "ecr" {
  source = "./modules/ecr"

  project     = var.project
  environment = var.environment

  repository_names = local.ecr_repositories
  max_image_count  = var.ecr_max_image_count

  tags = local.common_tags
}

################################################################################
# AWS Load Balancer Controller
################################################################################

module "alb_controller" {
  source = "./modules/alb-controller"

  project     = var.project
  environment = var.environment

  cluster_name      = module.eks.cluster_name
  oidc_provider_arn = module.eks.oidc_provider_arn
  oidc_provider_url = module.eks.oidc_provider_url
  vpc_id            = module.vpc.vpc_id
  aws_region        = var.aws_region
  chart_version     = var.alb_controller_chart_version

  tags = local.common_tags
}
