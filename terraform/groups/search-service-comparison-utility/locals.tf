locals {
  stack_name                 = "search-service"
  vault_secrets              = data.vault_generic_secret.configuration.data

  vpc_name                   = local.stack_secrets["vpc_name"]

  application_subnet_pattern = local.vault_secrets["application_subnet_pattern"]
  routing_subnet_pattern     = local.vault_secrets["routing_subnet_pattern"]

  lambda_vpc_access_subnet_ids = concat(
    data.aws_subnets.application.ids,
    data.aws_subnets.routing.ids
  )

  json_folder = "input_json/${var.aws_profile}/${var.environment}"

  lambda_cloudwatch_event_rules = [
    {
      name                = "${var.lambda_function_name}-${var.environment}-report"
      description         = "Trigger Lambda to generate a search comparison report"
      target_input        = data.local_file.report.content
    }
  ]

  # Secrets
  stack_secrets        = data.vault_generic_secret.stack_secrets.data
  stack_secrets_path   = "applications/${var.aws_profile}/${var.environment}/${local.stack_name}-stack"
  service_secrets      = data.vault_generic_secret.service_secrets.data
  service_secrets_path = "${local.stack_secrets_path}/${var.lambda_function_name}"

  additional_iam_policies_json = [data.aws_iam_policy_document.ssm_access_policy.json]
}
