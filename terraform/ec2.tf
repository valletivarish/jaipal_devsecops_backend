# EC2 Instance Configuration
# Provisions a t2.micro EC2 instance to host the FastAPI backend application.
# Includes a security group allowing SSH (22) and API (8000) access.
# User data script installs Python 3.11 and sets up the application as a systemd service.

# Security group for the EC2 instance
# Allows inbound SSH for management and port 8000 for the FastAPI API
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

  # Allow API access (port 8000) for the FastAPI backend
  ingress {
    from_port   = 8000
    to_port     = 8000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "FastAPI backend API"
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

# EC2 instance for hosting the FastAPI backend
resource "aws_instance" "backend" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  subnet_id              = aws_subnet.public_a.id
  vpc_security_group_ids = [aws_security_group.ec2_sg.id]
  key_name               = var.key_name

  # User data script runs on first boot to set up the server
  # Installs Python 3.11, pip, and creates a systemd service for the API
  user_data = <<-EOF
    #!/bin/bash
    # Update system packages
    dnf update -y

    # Install Python 3.11 and development tools
    dnf install -y python3.11 python3.11-pip git

    # Create application directory
    mkdir -p /home/ec2-user/app/backend
    chown -R ec2-user:ec2-user /home/ec2-user/app

    # Create systemd service for the FastAPI application
    cat > /etc/systemd/system/airquality-api.service <<SVCEOF
    [Unit]
    Description=Air Quality Monitoring API
    After=network.target

    [Service]
    Type=simple
    User=ec2-user
    WorkingDirectory=/home/ec2-user/app/backend
    ExecStart=/usr/bin/python3.11 -m uvicorn app.main:app --host 0.0.0.0 --port 8000
    Restart=always
    RestartSec=5
    Environment=DATABASE_URL=postgresql://${var.db_username}:${var.db_password}@${aws_db_instance.postgres.address}:5432/${var.db_name}

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
