# Terraform Variables
# Defines input variables for configuring AWS infrastructure.
# These variables allow customization without modifying the main configuration.
# Sensitive values (db_password, key_name) should be set via terraform.tfvars
# or environment variables, never committed to source control.

variable "aws_region" {
  description = "AWS region where resources will be created"
  type        = string
  default     = "eu-west-1"
}

variable "project_name" {
  description = "Project name used for resource naming and tagging"
  type        = string
  default     = "air-quality-monitor"
}

variable "instance_type" {
  description = "EC2 instance type for the backend server"
  type        = string
  default     = "t2.micro"
}

variable "db_instance_class" {
  description = "RDS instance class for the PostgreSQL database"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Name of the PostgreSQL database"
  type        = string
  default     = "air_quality_db"
}

variable "db_username" {
  description = "Master username for the RDS PostgreSQL instance"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "Master password for the RDS instance (set via environment variable)"
  type        = string
  sensitive   = true
}

variable "key_name" {
  description = "Name of the EC2 key pair for SSH access"
  type        = string
}

variable "jwt_secret" {
  description = "JWT signing secret for authentication (set via environment variable)"
  type        = string
  sensitive   = true
}
