# Offender Case Notes Service

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Foffender-case-notes)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/offender-case-notes "Link to report")
[![Docker Repository on GHCR](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/offender-case-notes)
[![Pipeline [test -> build -> deploy]](https://github.com/ministryofjustice/offender-case-notes/actions/workflows/pipeline.yml/badge.svg?branch=main)](https://github.com/ministryofjustice/offender-case-notes/actions/workflows/pipeline.yml)
[![codecov](https://codecov.io/github/ministryofjustice/offender-case-notes/graph/badge.svg?token=EQ8ADME9KY)](https://codecov.io/github/ministryofjustice/offender-case-notes)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://dev.offender-case-notes.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/offender-case-notes/main/async-api.yml&readOnly)

Datebase Schema diagram: https://ministryofjustice.github.io/offender-case-notes/schema-spy-report/

Service to provide secure access to retrieving and storing case notes about offenders

## Architectural Decision Records

For detailed insights into the architectural decisions made during the project to combine the DPS and NOMIS case notes, making this API the system of record for the latter, refer to our ADRs:

* [ADR001: Offender Case Notes API will become the system of record for all case notes data](architectural_decision_records/001-combine-nomis-and-dps-case-notes.md)
* [ADR002: Migrate full NOMIS case notes data set across all bookings](architectural_decision_records/002-migrate-full-case-notes-data-set.md)
* [ADR003: Adopting a Two-Way Sync for Prisoner Case Notes](architectural_decision_records/003-two-way-sync.md)
* [ADR004: Add new roles in line with agreed naming convention and remove reliance on DPS roles and user tokens](architectural_decision_records/004-roles.md)

## Running locally

[Running the service locally using run-local.sh](running-locally.md)

## Performance Tests

A basic performance test has been ran and is in the project to enable rerunning as a way of monitoring performance.
The performance tests are intended to be ran from a local machine. 

[How to run performance tests from a local machine](performance-testing.md)
