# S3 Bucket Configuration
# Provisions an S3 bucket for hosting the React frontend as a static website.
# Enables static website hosting with index.html as the entry point.
# Configures public read access for the website content.

# S3 bucket for frontend static website hosting
resource "aws_s3_bucket" "frontend" {
  bucket = "${var.project_name}-frontend-${var.aws_region}"

  tags = {
    Name    = "${var.project_name}-frontend"
    Project = var.project_name
  }
}

# Enable static website hosting on the S3 bucket
# index.html serves as the entry point for the React SPA
# error.html redirects to index.html for client-side routing support
resource "aws_s3_bucket_website_configuration" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "index.html"
  }
}

# Disable block public access settings to allow website hosting
resource "aws_s3_bucket_public_access_block" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

# Bucket policy allowing public read access to all objects
# Required for S3 static website hosting to serve files to browsers
resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.frontend.arn}/*"
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.frontend]
}
