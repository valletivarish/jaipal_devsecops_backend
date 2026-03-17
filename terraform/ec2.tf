# EC2 Instance Configuration
# Provisions a t2.micro EC2 instance to host the Spring Boot backend application.
# Includes a security group allowing SSH (22) and API (10004) access.
# User data script installs Java 17 and sets up the application as a systemd service.

# Security group for the EC2 instance
# Allows inbound SSH for management and port 10004 for the Spring Boot API
resource "aws_security_group" "ec2_sg" {
  name        = "${var.project_name}-ec2-sg"
  description = "Security group for backend EC2 instance"
  vpc_id      = aws_vpc.main.id

  # Allow SSH access (port 22) for deployment and management
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH access"
  }

  # Allow API access (port 10004) for the Spring Boot backend
  ingress {
    from_port   = 10004
    to_port     = 10004
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Spring Boot backend API"
  }

  # Allow HTTP access (port 80) for the React frontend served via nginx
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP access for frontend"
  }

  # Allow all outbound traffic for package installations and updates
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name    = "${var.project_name}-ec2-sg"
    Project = var.project_name
  }
}

# Look up the latest Amazon Linux 2023 AMI
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# EC2 instance for hosting the Spring Boot backend
resource "aws_instance" "backend" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  key_name               = var.key_name

  # User data script runs on first boot to set up the server
  # Installs Java 17, Maven, and creates a systemd service for the API
  user_data = <<-EOF
    #!/bin/bash
    # Update system packages
    dnf update -y

    # Install Java 17 and development tools
    dnf install -y java-17-amazon-corretto-devel maven git

    # Create application directory
    mkdir -p /home/ec2-user/app/backend
    chown -R ec2-user:ec2-user /home/ec2-user/app

    # Create systemd service for the Spring Boot application
    cat > /etc/systemd/system/airquality-api.service <<SVCEOF
    [Unit]
    Description=Air Quality Monitoring API (Spring Boot)
    After=network.target

    [Service]
    Type=simple
    User=ec2-user
    WorkingDirectory=/home/ec2-user/app/backend
    ExecStart=/usr/bin/java -jar target/air-quality-monitoring-1.0.0.jar --server.port=10004
    Restart=always
    RestartSec=5
    Environment=SPRING_DATASOURCE_URL=jdbc:postgresql://${aws_db_instance.postgres.address}:5432/${var.db_name}
    Environment=SPRING_DATASOURCE_USERNAME=${var.db_username}
    Environment=SPRING_DATASOURCE_PASSWORD=${var.db_password}
    Environment=JWT_SECRET=${var.jwt_secret}
    Environment=CORS_ALLOWED_ORIGINS=http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}

    [Install]
    WantedBy=multi-user.target
    SVCEOF

    systemctl daemon-reload
    systemctl enable airquality-api
  EOF

  tags = {
    Name    = "${var.project_name}-backend"
    Project = var.project_name
  }
}

# Elastic IP for the EC2 instance - provides a static public IP
# that persists across instance stop/start cycles
resource "aws_eip" "backend" {
  instance = aws_instance.backend.id
  domain   = "vpc"

  tags = {
    Name    = "${var.project_name}-eip"
    Project = var.project_name
  }
}
