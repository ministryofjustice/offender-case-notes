#
# This script is used to run the Case Notes API locally, to interact with local PostgreSQL and localstack containers.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -local profile (from application-local.yml). The latter overrides some of the defaults.
#

# AWS configuration
export AWS_REGION=eu-west-2

# Client credentials from environment variables
export OFFENDER_CASE_NOTES_CLIENT_ID="$HMPPS_CASE_NOTES_CLIENT_ID"
export OFFENDER_CASE_NOTES_CLIENT_SECRET="$HMPPS_CASE_NOTES_CLIENT_SECRET"

# Run the application with stdout and local profiles active
SPRING_PROFILES_ACTIVE=stdout,local ./gradlew bootRun

# End
