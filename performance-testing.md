# Performance Tests

The tests are ran using gradle to reduce the overhead in running performance tests.

There are two ways to run the tests:

1) Getting a token as part of the test - no time limit on the tests running
2) Providing a token before the tests start - tests must complete before the token expires

In order to run the test the following command can be issued:  
`BASE_URL=<url_of_service> AUTH_URL=<auth_url/auth/oauth/token> CLIENT_SECRET=<client_secret> CLIENT_ID=<client_id> ENVIRONMENT_NAME=dev ./gradlew gatlingRun`

For example, to run against dev grabbing a token for each user:
`BASE_URL=https://dev.offender-case-notes.service.justice.gov.uk AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth/oauth/token CLIENT_SECRET='**REDACTED**' CLIENT_ID='**REDACTED**' ENVIRONMENT_NAME=dev ./gradlew gatlingRun`

To run against preprod, please provide a token by adding 

`AUTH_TOKEN='<jwt_token>'` as an environment variable before the `./gradlew gatlingRun` command and update the BASE_URL to point to the preprod service.

Additionally, you will need to create a `person-identifiers-preprod.csv` file with a selection of identifiers to run (see the dev version for an example of layout).

After running the tests, the results can be viewed by opening `build/reports/gatling/`. 
This folder contains all the runs you may have unless, usually the most recent is the bottom one in the list. If you have only ran once or ran a clean there will be only one.
After opening the correct simulation folder, opening the `index.html` file in a browser will allow you to view the test report.

More accurate response times will be available in app insights as the results in the gatling report will be affected by latency, using the VPN and the ingress.