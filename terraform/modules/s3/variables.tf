variable "environment" {
  description = "Environment name"
  type        = string
}

variable "project_name" {
  description = "Project name (used in bucket naming)"
  type        = string
}

variable "log_retention_days" {
  description = "Number of days to retain logs in S3"
  type        = number
  default     = 90
}

variable "backup_retention_days" {
  description = "Number of days to retain backups in S3"
  type        = number
  default     = 365
}

variable "enable_replication" {
  description = "Enable S3 replication for backups bucket"
  type        = bool
  default     = false
}

variable "replication_bucket_arn" {
  description = "ARN of the destination bucket for replication"
  type        = string
  default     = ""
}

variable "tags" {
  description = "Common tags"
  type        = map(string)
  default     = {}
}
