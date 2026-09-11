terraform {
  required_version = ">= 1.3.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.72.0, < 6.0"
    }
    vault = {
      source  = "hashicorp/vault"
      version = ">= 5.0, < 6.0"
    }
  }

  backend "s3" {}
}

provider "aws" {
  region  = var.region
}

module "lambda" {
  source = "git@github.com:companieshouse/terraform-modules.git//aws/lambda?ref=1.0.373"

  environment            = var.environment
  function_name          = var.lambda_function_name
  lambda_runtime         = var.lambda_runtime
  lambda_handler         = var.lambda_handler_name

  lambda_code_s3_bucket  = var.release_bucket_name
  lambda_code_s3_key     = var.release_artifact_key

  lambda_memory_size                    = var.lambda_memory_size
  lambda_timeout_seconds                = var.lambda_timeout_seconds
  lambda_logs_retention_days            = var.lambda_logs_retention_days

  lambda_env_vars = {
    BLUE_SEARCH_CLUSTER_URL             = local.vault_secrets["blue_search_cluster_url"]
    GREEN_SEARCH_CLUSTER_URL            = local.vault_secrets["green_search_cluster_url"]
  }

  lambda_cloudwatch_event_rules = local.lambda_cloudwatch_event_rules
  additional_policies = local.additional_iam_policies_json

  lambda_sg_egress_rule = {
    from_port   = -1
    to_port     = -1
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lambda_vpc_access_subnet_ids         = local.lambda_vpc_access_subnet_ids
  lambda_vpc_id                        = data.aws_vpc.vpc.id
}
