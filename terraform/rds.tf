# RDS PostgreSQL Configuration
# Provisions a managed PostgreSQL database instance in a private subnet.
# The database is not publicly accessible - only the EC2 instance can connect.
# Uses db.t3.micro instance class for development/testing workloads.

# DB subnet group - places the RDS instance in private subnets
resource "aws_db_subnet_group" "main" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = [aws_subnet.private_a.id, aws_subnet.private_b.id]

  tags = {
    Name    = "${var.project_name}-db-subnet-group"
    Project = var.project_name
  }
}

# Security group for the RDS instance
# Only allows PostgreSQL connections from the EC2 security group
resource "aws_security_group" "rds_sg" {
  name        = "${var.project_name}-rds-sg"
  description = "Security group for RDS PostgreSQL instance"
  vpc_id      = aws_vpc.main.id

  # Allow PostgreSQL connections only from the EC2 backend server
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_sg.id]
    description     = "PostgreSQL access from EC2 backend"
  }

  tags = {
    Name    = "${var.project_name}-rds-sg"
    Project = var.project_name
  }
}

# RDS PostgreSQL instance
# Stores all application data: users, monitoring zones, readings, alerts
resource "aws_db_instance" "postgres" {
  identifier     = "${var.project_name}-db"
  engine         = "postgres"
  engine_version = "15"
  instance_class = var.db_instance_class

  # Database configuration
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  # Storage configuration - 20GB with autoscaling up to 100GB
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"

  # Network configuration - placed in private subnet, not publicly accessible
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]
  publicly_accessible    = false

  # Backup and maintenance settings
  backup_retention_period = 1
  skip_final_snapshot     = true

  tags = {
    Name    = "${var.project_name}-postgres"
    Project = var.project_name
  }
}
