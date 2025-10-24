/**
 * Development Environment Configuration
 * Uses cost-optimized settings: t3.micro instances, single-AZ where possible
 */

terraform {
  required_version = ">= 1.5.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

  backend "s3" {
    bucket         = "order-processing-terraform-state"
    key            = "dev/terraform.tfstate"
    region         = "ap-southeast-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = local.common_tags
  }
}

locals {
  environment = "dev"
  project_name = "order-processing"

  common_tags = {
    Environment = local.environment
    Project     = local.project_name
    ManagedBy   = "Terraform"
  }

  # Service configuration
  services = {
    order-service = {
      container_port     = 8080
      health_check_path  = "/actuator/health"
      path_patterns      = ["/api/orders*"]
      priority           = 100
    }
    inventory-service = {
      container_port     = 8081
      health_check_path  = "/actuator/health"
      path_patterns      = ["/api/inventory*"]
      priority           = 200
    }
  }
}

# VPC Module
module "vpc" {
  source = "../../modules/vpc"

  environment        = local.environment
  vpc_cidr          = var.vpc_cidr
  availability_zones = var.availability_zones
  enable_nat_gateway = true
  enable_flow_logs  = false # Disable for cost savings in dev

  tags = local.common_tags
}

# RDS Module
module "rds" {
  source = "../../modules/rds"

  environment                = local.environment
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_groups    = [module.ecs.ecs_instance_security_group_id]

  instance_class             = "db.t3.micro"
  engine_version            = "15.4"
  allocated_storage         = 20
  storage_type              = "gp3"
  database_name             = "orderdb"
  master_username           = "dbadmin"

  multi_az                  = false # Single-AZ for dev
  backup_retention_period   = 3     # Shorter retention for dev
  skip_final_snapshot       = true  # Skip final snapshot in dev

  monitoring_interval              = 0     # Disable enhanced monitoring for cost
  performance_insights_enabled     = false # Disable for cost
  deletion_protection              = false # Allow deletion in dev

  tags = local.common_tags
}

# ElastiCache Module
module "elasticache" {
  source = "../../modules/elasticache"

  environment                = local.environment
  vpc_id                     = module.vpc.vpc_id
  private_subnet_ids         = module.vpc.private_subnet_ids
  allowed_security_groups    = [module.ecs.ecs_instance_security_group_id]

  node_type                  = "cache.t3.micro"
  engine_version            = "7.0"
  num_cache_nodes           = 1 # Single node for dev
  automatic_failover_enabled = false
  multi_az_enabled          = false

  auth_token_enabled        = true
  snapshot_retention_limit  = 1 # Minimal retention for dev

  enable_cloudwatch_alarms  = false # Disable alarms for dev

  tags = local.common_tags
}

# S3 Module
module "s3" {
  source = "../../modules/s3"

  environment           = local.environment
  project_name          = local.project_name
  log_retention_days    = 30  # Shorter retention for dev
  backup_retention_days = 90  # Shorter retention for dev
  enable_replication    = false

  tags = local.common_tags
}

# ECS Module
module "ecs" {
  source = "../../modules/ecs"

  environment         = local.environment
  vpc_id              = module.vpc.vpc_id
  public_subnet_ids   = module.vpc.public_subnet_ids
  private_subnet_ids  = module.vpc.private_subnet_ids

  instance_type       = "t3.micro"
  asg_min_size        = 1
  asg_max_size        = 2
  asg_desired_capacity = 1

  enable_container_insights   = false # Disable for cost in dev
  enable_deletion_protection  = false

  services = local.services

  tags = local.common_tags
}
