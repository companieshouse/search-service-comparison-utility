# search-service-comparison-utility

Java 21 AWS Lambda utility for comparing search data between Blue (Elasticsearch) and Green (OpenSearch) clusters.

## Purpose
This project helps identify differences between search backends by running comparison operations and aggregating their results.

## Overview

This project runs comparison operations (for example, document count and query matching) against two search backends and returns a normalized JSON response for analysis.

## Building

### Prerequisites
- Java 21
- Maven 3.8.0 or later

### Build Commands

```bash
# Build the Lambda function
make build

# Run tests
make test

# Clean build artifacts
make clean
```

## Project Structure

### ComparisonOrchestratorService

Routes each incoming operation to the matching `ComparisonService` implementation and combines results.

### ComparisonService Implementations

Contain operation-specific comparison logic.

### ReportUploadService

Handles report upload to S3 when enabled in the orchestration flow.

## Runtime Requirements

- Java 21
- Maven 3.8.0 or later
- Network access to both search clusters

## Configuration

The Lambda function requires the following environment variables:

- `BLUE_SEARCH_CLUSTER_URL`: The URL of the Elasticsearch (blue) cluster (e.g., `https://vpc-es7-cluster-cidev-jadiwcsqw4i4ylwzqubep7r3ki.eu-west-2.es.amazonaws.com`)
- `GREEN_SEARCH_CLUSTER_URL`: The URL of the OpenSearch (green) cluster (e.g., `https://vpc-cidev-alphabetical-search-cn3moiropposzbo2nzqwj2gnhu.eu-west-2.es.amazonaws.com`)

Example values are environment-specific and should be provided through deployment configuration.

## How Orchestration Works

1. Validate the event payload.
2. Iterate through the `details` array.
3. Resolve the operation to the appropriate `ComparisonService`.
4. Execute each comparison service.
5. Aggregate and consolidate all comparison results.
6. Generate a single combined response.
7. Persist the report to S3.
8. Return the consolidated comparison response.

## Event Contract

### Example Request

```json
{
  "details": [
    {
      "action": "compare",
      "operation": "DOCUMENT_COUNT",
      "query": null,
      "size": 0
    },
    {
      "action": "compare",
      "operation": "MATCH_QUERY",
      "query": " GIRLONTHEGROUND ",
      "size": 100
    }
  ]
}
```
## Testing and Lambda Execution

### Test Execution Steps

#### Step 1: Access AWS

Log in to the AWS Access Portal and select the **Development** account.

#### Step 2: Open Lambda Console

Navigate to:

```text
AWS Console → Lambda → Event Functions
```

#### Step 3: Open the Lambda Function

Locate and open:

```text
search-service-comparison-utility-cidev
```

#### Step 4: Execute a Test Event

1. Open the **Test** tab.
2. Paste the required JSON payload into the **Event JSON** section.
3. Click **Test**.

Example:

```json
{
  "details": [
    {
      "action": "compare",
      "operation": "DOCUMENT_COUNT",
      "query": null,
      "size": 0
    },
    {
      "action": "compare",
      "operation": "MATCH_QUERY",
      "query": " GIRLONTHEGROUND ",
      "size": 100
    }
  ]
}
```

#### Step 5: Verify Successful Execution

Confirm the Lambda execution completed successfully.

Expected message:

```text
Executing function: succeeded
```

#### Step 6: Review CloudWatch Logs

Open the **CloudWatch Logs** link from the Lambda execution results and verify the following:

- Request received successfully.
- Comparison orchestration executed.
- Document count comparison completed.
- Match query comparison completed.
- Comparison results consolidated successfully.
- S3 report upload completed successfully.

#### Step 7: Verify Report in S3

Navigate to the S3 bucket:

```text
search-comparison-reports
```

#### Step 8: Open Report Location

Browse to:

```text
comparison-report/
```

Verify a new report file has been created, for example:

```text
comparison-report/2026-10-16T09:17:48.525028142Z.json
```

#### Step 9: Download the Report

Download the generated JSON report from the S3 bucket.

#### Step 10: Validate Report Contents

Open the report using:

- Visual Studio Code
- Web browser
- JSON Viewer

Verify that:

- The request was processed successfully.
- All requested comparison operations are present.
- Document count results are included (when requested).
- Match query results are included (when requested).
- The consolidated response structure is correct.
- No unexpected errors are present.