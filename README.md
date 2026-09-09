# search-service-comparison-utility

A service that compares document counts and reports any discrepancies between the blue and green search clusters.

## Overview

This project contains a Lambda function that queries both Elasticsearch (blue cluster) and OpenSearch (green cluster) to retrieve document counts from the index and returns the results in a standardized JSON format.


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


## Configuration

The Lambda function requires the following environment variables:

- `BLUE_SEARCH_CLUSTER_URL`: The URL of the Elasticsearch (blue) cluster (e.g., `https://vpc-es7-cluster-cidev-jadiwcsqw4i4ylwzqubep7r3ki.eu-west-2.es.amazonaws.com`)
- `GREEN_SEARCH_CLUSTER_URL`: The URL of the OpenSearch (green) cluster (e.g., `https://vpc-cidev-alphabetical-search-cn3moiropposzbo2nzqwj2gnhu.eu-west-2.es.amazonaws.com`)

