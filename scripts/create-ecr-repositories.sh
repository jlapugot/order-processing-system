#!/bin/bash
set -e

# Create ECR Repositories for Docker images
# This should be run before the first deployment

AWS_REGION=${AWS_REGION:-ap-southeast-1}

REPOSITORIES=(
  "order-service"
  "inventory-service"
)

echo "Creating ECR repositories in region: $AWS_REGION"

for repo in "${REPOSITORIES[@]}"; do
  echo "Creating repository: $repo"

  aws ecr create-repository \
    --repository-name $repo \
    --region $AWS_REGION \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256 \
    --tags Key=Project,Value=order-processing Key=ManagedBy,Value=Script \
    2>/dev/null || echo "Repository $repo already exists"

  # Set lifecycle policy to clean up old images
  echo "Setting lifecycle policy for $repo"
  aws ecr put-lifecycle-policy \
    --repository-name $repo \
    --region $AWS_REGION \
    --lifecycle-policy-text '{
      "rules": [
        {
          "rulePriority": 1,
          "description": "Keep last 10 images",
          "selection": {
            "tagStatus": "any",
            "countType": "imageCountMoreThan",
            "countNumber": 10
          },
          "action": {
            "type": "expire"
          }
        }
      ]
    }' >/dev/null

  echo "✅ Repository $repo configured"
done

echo ""
echo "✅ All ECR repositories created successfully!"
echo ""
echo "Repository URLs:"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
for repo in "${REPOSITORIES[@]}"; do
  echo "$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$repo"
done
