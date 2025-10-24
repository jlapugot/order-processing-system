#!/bin/bash
set -e

# Deploy Infrastructure with Terraform
# Usage: ./deploy-infrastructure.sh <environment>
# Example: ./deploy-infrastructure.sh dev

ENVIRONMENT=${1:-dev}
TERRAFORM_DIR="terraform/environments/$ENVIRONMENT"

if [ ! -d "$TERRAFORM_DIR" ]; then
  echo "❌ Error: Environment '$ENVIRONMENT' not found"
  echo "Available environments: dev, prod"
  exit 1
fi

echo "Deploying infrastructure for environment: $ENVIRONMENT"

cd $TERRAFORM_DIR

# Initialize Terraform
echo "Initializing Terraform..."
terraform init

# Validate configuration
echo "Validating Terraform configuration..."
terraform validate

# Format code
echo "Formatting Terraform code..."
terraform fmt -recursive

# Plan deployment
echo "Planning deployment..."
terraform plan -out=tfplan

# Prompt for approval
if [ "$ENVIRONMENT" == "prod" ]; then
  read -p "⚠️  You are about to deploy to PRODUCTION. Continue? (yes/no): " confirm
  if [ "$confirm" != "yes" ]; then
    echo "Deployment cancelled"
    exit 0
  fi
fi

# Apply deployment
echo "Applying deployment..."
terraform apply tfplan

# Clean up plan file
rm -f tfplan

# Show outputs
echo ""
echo "✅ Deployment complete!"
echo ""
echo "Outputs:"
terraform output

echo ""
echo "Next steps:"
echo "1. Create ECS task definitions"
echo "2. Create ECS services"
echo "3. Deploy applications using GitHub Actions"
