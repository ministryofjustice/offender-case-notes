# Running the service locally using run-local.sh
This will run the service locally. It starts the database and localstack containers then start the service via a bash script.
It connects to the dev versions of all dependent services.

## Environment variables

The script expects the following environment variables to be set:

```
HMPPS_CASE_NOTES_CLIENT_ID
HMPPS_CASE_NOTES_CLIENT_SECRET
```

These environment variables should be set to the dev secrets values. Remember to escape any `$` characters with `\$`.

Command line for retrieving secrets in k8s dev namespace:
```kubectl -n <dev-namespace-here> get secret <secret-name-here> -o json | jq -r ".data | map_values(@base64d)"```

## Running the service locally

Run the following commands from the root directory of the project:

1. docker compose up -d
2. ./run-local.sh
