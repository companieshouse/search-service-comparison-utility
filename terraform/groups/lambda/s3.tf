resource "aws_s3_bucket" "search_comparison_reports" {
  bucket = local.s3_bucket_name
}
