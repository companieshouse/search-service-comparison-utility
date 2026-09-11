data "vault_generic_secret" "configuration" {
  path = local.service_secrets_path
}

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

data "aws_subnets" "application" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.vpc.id]
  }

  filter {
    name = "tag:Name"
    values = [local.application_subnet_pattern]
  }
}

data "aws_subnets" "routing" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.vpc.id]
  }

  filter {
    name = "tag:Name"
    values = [local.routing_subnet_pattern]
  }
}

data "aws_kms_key" "kms_key" {
  key_id = local.vault_secrets["file_transfer_service_kms_key_alias"]
}

# Policy to allow Lambda to access SSM Parameter Store
# TODO: Should the scope be reduced? Prefixed?
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