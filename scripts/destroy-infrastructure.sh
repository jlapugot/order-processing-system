#!/bin/bash
set -e

# Destroy Infrastructure with Terraform
# Usage: ./destroy-infrastructure.sh <environment>
# Example: ./destroy-infrastructure.sh dev

ENVIRONMENT=${1:-dev}
TERRAFORM_DIR="terraform/environments/$ENVIRONMENT"

if [ ! -d "$TERRAFORM_DIR" ]; then
  echo "❌ Error: Environment '$ENVIRONMENT' not found"
  echo "Available environments: dev, prod"
  exit 1
fi

echo "⚠️  WARNING: You are about to DESTROY all infrastructure for: $ENVIRONMENT"

# Extra confirmation for production
if [ "$ENVIRONMENT" == "prod" ]; then
  echo "⚠️⚠️⚠️  THIS IS THE PRODUCTION ENVIRONMENT ⚠️⚠️⚠️"
  read -p "Type 'destroy-production' to confirm: " confirm
  if [ "$confirm" != "destroy-production" ]; then
    echo "Destruction cancelled"
    exit 0
  fi
else
  read -p "Type 'yes' to confirm: " confirm
  if [ "$confirm" != "yes" ]; then
    echo "Destruction cancelled"
    exit 0
  fi
fi

cd $TERRAFORM_DIR

# Initialize Terraform
echo "Initializing Terraform..."
terraform init

# Plan destruction
echo "Planning destruction..."
terraform plan -destroy -out=tfplan

# Apply destruction
echo "Destroying infrastructure..."
terraform apply tfplan

# Clean up
rm -f tfplan

echo "✅ Infrastructure destroyed for environment: $ENVIRONMENT"
