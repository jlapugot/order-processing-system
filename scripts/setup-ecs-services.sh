#!/bin/bash
set -e

# Create ECS Task Definitions and Services
# Usage: ./setup-ecs-services.sh <environment>
# Example: ./setup-ecs-services.sh dev

ENVIRONMENT=${1:-dev}
AWS_REGION=${AWS_REGION:-ap-southeast-1}

echo "Setting up ECS services for environment: $ENVIRONMENT"

# Get outputs from Terraform
cd terraform/environments/$ENVIRONMENT

CLUSTER_NAME=$(terraform output -raw ecs_cluster_name)
DB_ENDPOINT=$(terraform output -raw rds_endpoint)
REDIS_ENDPOINT=$(terraform output -raw redis_endpoint)
DB_SECRET_ARN=$(terraform output -raw db_password_secret_arn)
REDIS_SECRET_ARN=$(terraform output -raw redis_auth_token_secret_arn)
TASK_EXECUTION_ROLE=$(terraform output -raw task_execution_role_arn)
TASK_ROLE=$(terraform output -raw task_role_arn)
TARGET_GROUP_ORDER=$(terraform output -raw target_group_arns | jq -r '.["order-service"]')
TARGET_GROUP_INVENTORY=$(terraform output -raw target_group_arns | jq -r '.["inventory-service"]')

cd ../../..

# Get ECR repository URLs
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_ORDER="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/order-service:latest"
ECR_INVENTORY="$ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/inventory-service:latest"

echo "Creating Order Service task definition..."

# Create Order Service task definition
cat > /tmp/order-task-def.json <<EOF
{
  "family": "$ENVIRONMENT-order-service",
  "networkMode": "bridge",
  "requiresCompatibilities": ["EC2"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "$TASK_EXECUTION_ROLE",
  "taskRoleArn": "$TASK_ROLE",
  "containerDefinitions": [
    {
      "name": "order-service",
      "image": "$ECR_ORDER",
      "cpu": 512,
      "memory": 1024,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "$ENVIRONMENT"
        },
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://$DB_ENDPOINT/orderdb"
        },
        {
          "name": "SPRING_REDIS_HOST",
          "value": "$REDIS_ENDPOINT"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "$DB_SECRET_ARN"
        },
        {
          "name": "SPRING_REDIS_PASSWORD",
          "valueFrom": "$REDIS_SECRET_ARN"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/$ENVIRONMENT",
          "awslogs-region": "$AWS_REGION",
          "awslogs-stream-prefix": "order-service"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
EOF

aws ecs register-task-definition --cli-input-json file:///tmp/order-task-def.json

echo "Creating Inventory Service task definition..."

# Create Inventory Service task definition
cat > /tmp/inventory-task-def.json <<EOF
{
  "family": "$ENVIRONMENT-inventory-service",
  "networkMode": "bridge",
  "requiresCompatibilities": ["EC2"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "$TASK_EXECUTION_ROLE",
  "taskRoleArn": "$TASK_ROLE",
  "containerDefinitions": [
    {
      "name": "inventory-service",
      "image": "$ECR_INVENTORY",
      "cpu": 512,
      "memory": 1024,
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8081,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "$ENVIRONMENT"
        },
        {
          "name": "SPRING_DATASOURCE_URL",
          "value": "jdbc:postgresql://$DB_ENDPOINT/orderdb"
        },
        {
          "name": "SPRING_REDIS_HOST",
          "value": "$REDIS_ENDPOINT"
        }
      ],
      "secrets": [
        {
          "name": "SPRING_DATASOURCE_PASSWORD",
          "valueFrom": "$DB_SECRET_ARN"
        },
        {
          "name": "SPRING_REDIS_PASSWORD",
          "valueFrom": "$REDIS_SECRET_ARN"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/$ENVIRONMENT",
          "awslogs-region": "$AWS_REGION",
          "awslogs-stream-prefix": "inventory-service"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8081/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
EOF

aws ecs register-task-definition --cli-input-json file:///tmp/inventory-task-def.json

echo "Creating Order Service..."

aws ecs create-service \
  --cluster $CLUSTER_NAME \
  --service-name $ENVIRONMENT-order-service \
  --task-definition $ENVIRONMENT-order-service \
  --desired-count 2 \
  --launch-type EC2 \
  --load-balancers "targetGroupArn=$TARGET_GROUP_ORDER,containerName=order-service,containerPort=8080" \
  --health-check-grace-period-seconds 60 \
  --deployment-configuration "minimumHealthyPercent=50,maximumPercent=200" \
  || echo "Service already exists"

echo "Creating Inventory Service..."

aws ecs create-service \
  --cluster $CLUSTER_NAME \
  --service-name $ENVIRONMENT-inventory-service \
  --task-definition $ENVIRONMENT-inventory-service \
  --desired-count 2 \
  --launch-type EC2 \
  --load-balancers "targetGroupArn=$TARGET_GROUP_INVENTORY,containerName=inventory-service,containerPort=8081" \
  --health-check-grace-period-seconds 60 \
  --deployment-configuration "minimumHealthyPercent=50,maximumPercent=200" \
  || echo "Service already exists"

# Clean up temp files
rm -f /tmp/order-task-def.json /tmp/inventory-task-def.json

echo "✅ ECS services created successfully!"
echo ""
echo "Monitor services with:"
echo "aws ecs describe-services --cluster $CLUSTER_NAME --services $ENVIRONMENT-order-service $ENVIRONMENT-inventory-service"
