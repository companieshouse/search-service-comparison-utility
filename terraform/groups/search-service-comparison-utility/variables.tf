variable "aws_account" {
  type        = string
  description = "The AWS account name"
  default     = "development"
}

variable "aws_profile" {
  type        = string
  description = "The AWS profile name; used as a prefix for Vault secrets"
}

variable "region" {
  type        = string
  description = "The AWS region in which resources will be created"
  default     = "eu-west-2"
}

variable "environment" {
  type        = string
  description = "The environment name to be used when creating AWS resources"
}

variable "lambda_function_name" {
  type        = string
  description = "The name of the Lambda function"
  default     = "search-service-comparison-utility"
}

variable "lambda_handler_name" {
  type        = string
  description = "The lambda function entrypoint"
  # TODO - UPDATE THIS
  #default     = "uk.gov.companieshouse.efs.documentconverter.DocumentMessageHandler::handleRequest" 
}

variable "lambda_logs_retention_days" {
  type        = number
  description = "The number of days to retain Lambda logs in CloudWatch"
  default     = 7
}

variable "lambda_memory_size" {
  type        = string
  description = "The amount of memory made available to the Lambda function at runtime in megabytes"
  default     = "4096"
}

variable "lambda_timeout_seconds" {
  type        = string
  description = "The amount of time the lambda function is allowed to run before being stopped"
  default     = 600
}

variable "lambda_runtime" {
  type        = string
  description = "The lambda runtime to use for the function"
  default     = "java21"
}

variable "release_bucket_name" {
  type        = string
  description = "The name of the S3 bucket containing the release artefact for the Lambda function"
}

variable "release_artifact_key" {
  type        = string
  description = "The release artifact key for the Lambda function"
}

variable "network_state_bucket_name" {
  type        = string
  description = "The name of the S3 bucket containing the application network remote state"
}

variable "network_state_bucket_key" {
  type        = string
  description = "The key name used when constructing the path to the application network remote state in the S3 bucket"
}
