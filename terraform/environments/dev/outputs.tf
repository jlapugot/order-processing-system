output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = module.ecs.alb_dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = module.ecs.cluster_name
}

output "rds_endpoint" {
  description = "RDS endpoint"
  value       = module.rds.db_instance_endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = module.elasticache.redis_primary_endpoint
  sensitive   = true
}

output "artifacts_bucket" {
  description = "Artifacts S3 bucket name"
  value       = module.s3.artifacts_bucket_id
}

output "db_password_secret_arn" {
  description = "ARN of DB password secret"
  value       = module.rds.db_password_secret_arn
}

output "redis_auth_token_secret_arn" {
  description = "ARN of Redis auth token secret"
  value       = module.elasticache.redis_auth_token_secret_arn
}
