resource "aws_s3_bucket" "search_comparison_reports" {
  bucket = local.s3_bucket_name
}

resource "aws_s3_bucket_versioning" "search_comparison_reports" {
  bucket = aws_s3_bucket.search_comparison_reports.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "search_comparison_reports" {
  bucket = aws_s3_bucket.search_comparison_reports.id

  rule {
    id     = "delete-search-search_comparison_reports"
    status = "Enabled"

    expiration {
      days = local.s3_report_retention_days
    }

    filter {}

    noncurrent_version_expiration {
      noncurrent_days = local.s3_report_retention_days
    }
  }
}
