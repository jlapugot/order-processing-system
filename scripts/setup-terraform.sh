#!/bin/bash
set -e

# Setup Terraform State Backend
# This script creates the S3 bucket and DynamoDB table for Terraform state management

AWS_REGION=${AWS_REGION:-ap-southeast-1}
STATE_BUCKET="order-processing-terraform-state"
LOCK_TABLE="terraform-state-lock"

echo "Setting up Terraform state backend..."

# Create S3 bucket for state
echo "Creating S3 bucket: $STATE_BUCKET"
aws s3api create-bucket \
  --bucket $STATE_BUCKET \
  --region $AWS_REGION \
  --create-bucket-configuration LocationConstraint=$AWS_REGION 2>/dev/null || echo "Bucket already exists"

# Enable versioning
echo "Enabling versioning on S3 bucket..."
aws s3api put-bucket-versioning \
  --bucket $STATE_BUCKET \
  --versioning-configuration Status=Enabled

# Enable encryption
echo "Enabling encryption on S3 bucket..."
aws s3api put-bucket-encryption \
  --bucket $STATE_BUCKET \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {
        "SSEAlgorithm": "AES256"
      },
      "BucketKeyEnabled": true
    }]
  }'

# Block public access
echo "Blocking public access on S3 bucket..."
aws s3api put-public-access-block \
  --bucket $STATE_BUCKET \
  --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Create DynamoDB table for state locking
echo "Creating DynamoDB table: $LOCK_TABLE"
aws dynamodb create-table \
  --table-name $LOCK_TABLE \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region $AWS_REGION 2>/dev/null || echo "Table already exists"

echo "✅ Terraform state backend setup complete!"
echo "S3 Bucket: $STATE_BUCKET"
echo "DynamoDB Table: $LOCK_TABLE"
