resource "aws_iam_policy" "lambda_policy" {
  name        = "${var.environment}-${var.lambda_function_name}-lambda-policy"
  description = "Custom policy for Lambda execution with S3 and KMS access."
  policy      = data.aws_iam_policy_document.lambda_execution.json
}
