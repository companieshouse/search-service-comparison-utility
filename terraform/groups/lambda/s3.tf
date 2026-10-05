resource "aws_s3_bucket" "search_comparison_reports" {
  bucket = local.s3_bucket_name
}

resource "aws_s3_bucket_versioning" "search_comparison_reports" {
  bucket = aws_s3_bucket.search_comparison_reports.id

  versioning_configuration {
    status = "Enabled"
  }
}
