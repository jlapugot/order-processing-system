/**
 * Production Environment Configuration
 * Uses production-ready settings: Multi-AZ, enhanced monitoring, deletion protection
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
    key            = "prod/terraform.tfstate"
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
  environment = "prod"
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
  enable_flow_logs  = true # Enable for production monitoring

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
  allocated_storage         = 50
  storage_type              = "gp3"
  database_name             = "orderdb"
  master_username           = "dbadmin"

  multi_az                  = true # Multi-AZ for high availability
  backup_retention_period   = 7
  skip_final_snapshot       = false # Create final snapshot before deletion

  monitoring_interval              = 60   # Enhanced monitoring
  performance_insights_enabled     = true # Enable Performance Insights
  deletion_protection              = true # Protect from accidental deletion

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
  num_cache_nodes           = 2 # Two nodes for high availability
  automatic_failover_enabled = true
  multi_az_enabled          = true

  auth_token_enabled        = true
  snapshot_retention_limit  = 5

  enable_cloudwatch_alarms  = true
  alarm_actions            = var.sns_topic_arns

  tags = local.common_tags
}

# S3 Module
module "s3" {
  source = "../../modules/s3"

  environment           = local.environment
  project_name          = local.project_name
  log_retention_days    = 90
  backup_retention_days = 365
  enable_replication    = var.enable_s3_replication
  replication_bucket_arn = var.replication_bucket_arn

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
  asg_min_size        = 2 # Minimum 2 instances for high availability
  asg_max_size        = 6
  asg_desired_capacity = 3

  enable_container_insights   = true # Enable Container Insights
  enable_deletion_protection  = true # Protect ALB from deletion

  services = local.services

  tags = local.common_tags
}
