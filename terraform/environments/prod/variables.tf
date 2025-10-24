variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-southeast-1"
}

variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
  default     = "10.1.0.0/16"
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
  default     = ["ap-southeast-1a", "ap-southeast-1b", "ap-southeast-1c"]
}

variable "sns_topic_arns" {
  description = "SNS topic ARNs for CloudWatch alarms"
  type        = list(string)
  default     = []
}

variable "enable_s3_replication" {
  description = "Enable S3 replication for disaster recovery"
  type        = bool
  default     = false
}

variable "replication_bucket_arn" {
  description = "ARN of S3 bucket for replication (if enabled)"
  type        = string
  default     = ""
}
