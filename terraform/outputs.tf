# Terraform Outputs
# Displays important URLs and connection information after infrastructure deployment.
# These values are needed for CI/CD configuration and application access.

# Backend EC2 instance public IP - used for API access and SSH
output "ec2_public_ip" {
  description = "Public IP address of the backend EC2 instance"
  value       = aws_instance.backend.public_ip
}

# Backend API URL - base URL for the FastAPI application
output "backend_url" {
  description = "URL for the FastAPI backend API"
  value       = "http://${aws_instance.backend.public_ip}:8000"
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

# RDS database endpoint - used in backend DATABASE_URL configuration
output "rds_endpoint" {
  description = "RDS PostgreSQL connection endpoint"
  value       = aws_db_instance.postgres.address
}

# RDS database port
output "rds_port" {
  description = "RDS PostgreSQL port"
  value       = aws_db_instance.postgres.port
}
