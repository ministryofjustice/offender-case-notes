# Offender Case Notes Service

[![CircleCI](https://circleci.com/gh/ministryofjustice/prison-to-nhs-update/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/offender-case-notes)
[![API docs](https://img.shields.io/badge/API_docs_(needs_VPN)-view-85EA2D.svg?logo=swagger)](https://dev.offender-case-notes.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/offender-case-notes/main/async-api.yml&readOnly)

Service to provide secure access to retrieving and storing case notes about offenders

## Architectural Decision Records

For detailed insights into the architectural decisions made during the project to combine the DPS and NOMIS case notes, making this API the system of record for the latter, refer to our ADRs:

* [ADR001: Offender Case Notes API will become the system of record for all case notes data](architectural_decision_records/001-combine-nomis-and-dps-case-notes.md)
* [ADR002: Migrate full NOMIS case notes data set across all bookings](architectural_decision_records/002-migrate-full-case-notes-data-set.md)
* [ADR003: Adopting a Two-Way Sync for Prisoner Case Notes](architectural_decision_records/003-two-way-sync.md)
* [ADR004: Add new roles in line with agreed naming convention and remove reliance on DPS roles and user tokens](architectural_decision_records/004-roles.md)

