# Terraform Outputs
# Displays important URLs and connection information after infrastructure deployment.
# These values are needed for CI/CD configuration and application access.

# Backend Elastic IP - static IP that persists across instance restarts
output "ec2_elastic_ip" {
  description = "Elastic IP address of the backend EC2 instance"
  value       = aws_eip.backend.public_ip
}

# Backend API URL - base URL for the Spring Boot application
output "backend_url" {
  description = "URL for the Spring Boot backend API"
  value       = "http://${aws_eip.backend.public_ip}:8080"
}

# Frontend website URL - S3 static website endpoint
output "frontend_url" {
  description = "URL for the React frontend (S3 static website)"
  value       = aws_s3_bucket_website_configuration.frontend.website_endpoint
}

# S3 bucket name - needed for CI/CD deployment command
output "s3_bucket_name" {
  description = "Name of the S3 bucket hosting the frontend"
  value       = aws_s3_bucket.frontend.id
}

# RDS database endpoint - used in backend SPRING_DATASOURCE_URL configuration
output "rds_endpoint" {
  description = "RDS PostgreSQL connection endpoint"
  value       = aws_db_instance.postgres.address
}

# RDS database port
output "rds_port" {
  description = "RDS PostgreSQL port"
  value       = aws_db_instance.postgres.port
}

# EC2 instance ID
output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.backend.id
}
