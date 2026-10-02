data "vault_generic_secret" "stack_secrets" {
  path = local.stack_secrets_path
}

data "vault_generic_secret" "service_secrets" {
  path = local.service_secrets_path
}

data "aws_vpc" "vpc" {
  filter {
    name   = "tag:Name"
    values = [local.vpc_name]
  }
}

# Get application subnet IDs
data "aws_subnets" "application" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.vpc.id]
  }

  filter {
    name   = "tag:Name"
    values = [local.application_subnet_pattern]
  }
}

data "aws_kms_key" "kms_key" {
  key_id = local.kms_alias
}

data "aws_caller_identity" "aws_identity" {}

# Policy to allow Lambda to access SSM Parameter Store
data "aws_iam_policy_document" "ssm_access_policy" {
  statement {
    sid    = "AllowSSMAccess"
    effect = "Allow"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
      "ssm:GetParameterHistory"
    ]
    resources = ["*"]
  }
}

data "local_file" "report" {
  filename = "${local.json_folder}/report.json"
}

data "aws_opensearch_domain" "opensearch" {
  for_each = toset(var.opensearch_domain_names)

  domain_name = "${var.environment}-${each.value}"
}

data "aws_iam_policy_document" "opensearch_access_policy" {
  statement {
    sid       = "AllowOpenSearchReadAccess"
    effect    = "Allow"
    actions   = ["es:ESHttpGet", "es:ESHttpPost", "es:ESHttpHead"]
    resources = [for domain in data.aws_opensearch_domain.opensearch : "${domain.arn}/*"]
  }
}

data "aws_s3_bucket" "search_comparison_reports_bucket" {
  bucket = local.s3_bucket_name
}

data "aws_iam_policy_document" "s3_access_policy" {
  statement {
    sid       = "AllowSearchComparisonBucketListAccess"
    effect    = "Allow"
    actions   = ["s3:ListBucket", "s3:GetBucketLocation"]
    resources = [data.aws_s3_bucket.search_comparison_reports_bucket.arn]
  }

  statement {
    sid       = "AllowSearchComparisonObjectAccess"
    effect    = "Allow"
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["${data.aws_s3_bucket.search_comparison_reports_bucket.arn}/*"]
  }
}
