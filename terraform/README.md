<!-- BEGIN_TF_DOCS -->
## Requirements

| Name | Version |
| ---- | ------- |
| <a name="requirement_terraform"></a> [terraform](#requirement\_terraform) | >= 1.3.0, < 2.0.0 |
| <a name="requirement_aws"></a> [aws](#requirement\_aws) | >= 5.72.0, < 6.0 |
| <a name="requirement_vault"></a> [vault](#requirement\_vault) | >= 3.18.0, < 5.0 |

## Providers

| Name | Version |
| ---- | ------- |
| <a name="provider_aws"></a> [aws](#provider\_aws) | 5.72.1 |
| <a name="provider_local"></a> [local](#provider\_local) | n/a |
| <a name="provider_vault"></a> [vault](#provider\_vault) | 3.18.0 |

## Modules

| Name | Source | Version |
| ---- | ------ | ------- |
| <a name="module_lambda"></a> [lambda](#module\_lambda) | git@github.com:companieshouse/terraform-modules.git//aws/lambda | 1.0.373 |

## Resources

| Name | Type |
| ---- | ---- |
| [aws_iam_policy.lambda_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/resources/iam_policy) | resource |
| [aws_iam_policy_document.ssm_access_policy](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/iam_policy_document) | data source |
| [aws_kms_key.kms_key](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/kms_key) | data source |
| [aws_subnets.application](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnets) | data source |
| [aws_subnets.routing](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/subnets) | data source |
| [aws_vpc.vpc](https://registry.terraform.io/providers/hashicorp/aws/latest/docs/data-sources/vpc) | data source |
| [local_file.report](https://registry.terraform.io/providers/hashicorp/local/latest/docs/data-sources/file) | data source |
| [vault_generic_secret.configuration](https://registry.terraform.io/providers/hashicorp/vault/latest/docs/data-sources/generic_secret) | data source |
| [vault_generic_secret.service_secrets](https://registry.terraform.io/providers/hashicorp/vault/latest/docs/data-sources/generic_secret) | data source |
| [vault_generic_secret.stack_secrets](https://registry.terraform.io/providers/hashicorp/vault/latest/docs/data-sources/generic_secret) | data source |

## Inputs

| Name | Description | Type | Default | Required |
| ---- | ----------- | ---- | ------- | :------: |
| <a name="input_aws_account"></a> [aws\_account](#input\_aws\_account) | The AWS account name | `string` | `"development"` | no |
| <a name="input_aws_profile"></a> [aws\_profile](#input\_aws\_profile) | The AWS profile name; used as a prefix for Vault secrets | `string` | n/a | yes |
| <a name="input_environment"></a> [environment](#input\_environment) | The environment name to be used when creating AWS resources | `string` | n/a | yes |
| <a name="input_hashicorp_vault_password"></a> [hashicorp\_vault\_password](#input\_hashicorp\_vault\_password) | The password used when retrieving configuration from Hashicorp Vault | `string` | n/a | yes |
| <a name="input_hashicorp_vault_username"></a> [hashicorp\_vault\_username](#input\_hashicorp\_vault\_username) | The username used when retrieving configuration from Hashicorp Vault | `string` | n/a | yes |
| <a name="input_lambda_function_name"></a> [lambda\_function\_name](#input\_lambda\_function\_name) | The name of the Lambda function | `string` | `"search-service-comparison-utility"` | no |
| <a name="input_lambda_handler_name"></a> [lambda\_handler\_name](#input\_lambda\_handler\_name) | The lambda function entrypoint | `string` | n/a | yes |
| <a name="input_lambda_logs_retention_days"></a> [lambda\_logs\_retention\_days](#input\_lambda\_logs\_retention\_days) | The number of days to retain Lambda logs in CloudWatch | `number` | `7` | no |
| <a name="input_lambda_memory_size"></a> [lambda\_memory\_size](#input\_lambda\_memory\_size) | The amount of memory made available to the Lambda function at runtime in megabytes | `string` | `"4096"` | no |
| <a name="input_lambda_runtime"></a> [lambda\_runtime](#input\_lambda\_runtime) | The lambda runtime to use for the function | `string` | `"java21"` | no |
| <a name="input_lambda_timeout_seconds"></a> [lambda\_timeout\_seconds](#input\_lambda\_timeout\_seconds) | The amount of time the lambda function is allowed to run before being stopped | `string` | `600` | no |
| <a name="input_network_state_bucket_key"></a> [network\_state\_bucket\_key](#input\_network\_state\_bucket\_key) | The key name used when constructing the path to the application network remote state in the S3 bucket | `string` | n/a | yes |
| <a name="input_network_state_bucket_name"></a> [network\_state\_bucket\_name](#input\_network\_state\_bucket\_name) | The name of the S3 bucket containing the application network remote state | `string` | n/a | yes |
| <a name="input_region"></a> [region](#input\_region) | The AWS region in which resources will be created | `string` | `"eu-west-2"` | no |
| <a name="input_release_artifact_key"></a> [release\_artifact\_key](#input\_release\_artifact\_key) | The release artifact key for the Lambda function | `string` | n/a | yes |
| <a name="input_release_bucket_name"></a> [release\_bucket\_name](#input\_release\_bucket\_name) | The name of the S3 bucket containing the release artefact for the Lambda function | `string` | n/a | yes |

## Outputs

No outputs.
<!-- END_TF_DOCS -->