# ADR003: Adopting a Two-Way Sync for Prisoner Case Notes

## Status

Approved by @Alice Noakes as Service Owner and @Richard Adams as Principal Technical Architect

## Context

The HMPPS strategy for decommissioning NOMIS by decomposing it into a set of microservices relies heavily on a sync capability provided by Syscon. The Syscon migrate and sync service suite coordinates the processes that keeps data held in DPS and NOMIS in sync. These services are event driven and can subscribe to events being published from DPS and/or NOMIS to drive synchronisation calls.

Each new service that is developed requires a decision on sync. Theoretically a service can choose not to sync any data however in practice, all NOMIS replacement services so far have adopted at least a one-way sync from DPS to NOMIS to support legacy clients and existing reports. This is usually supported by an initial migration of existing data into the new DPS service.

Developing the synchronisation support for a new service is not free. The mappings between the new services need defining, relevant events identifying or adding and endpoints to support sync creating in both the new service and the NOMIS Synchronisation API. This then needs thorough testing before considering deploying to production. This is a non-trivial amount of work and can be particularly complex if the data model of the two services differ significantly or there is any form of consolidating or flattening logic when syncing the data. It should be noted however that **Syscon themselves estimate that a two-way sync is only 10% more effort than a one-way sync**.

The HMPPS Technical Architect community has a preference for adopting one-way sync from DPS to NOMIS. This motivates teams to extract all business logic from NOMIS and simplifies the path to eventually decommissioning the NOMIS functionality and data completely. 

## Decision

The Move and Improve team is adopting a two-way sync between DPS and NOMIS during service roll out. The team will then document the dependencies that need resolving e.g. the creation of a case note when an alert is added before the service could move to a one-way sync.

In collaboration with Syscon, we will:

* Develop and deploy the migration process to copy the full NOMIS case notes data set as per [ADR002: Migrate full NOMIS case notes data set across all bookings](002-migrate-full-case-notes-data-set.md)
* Support prisoner case notes created and updated via the new service by syncing the changes back into NOMIS
* Support prisoner case notes created and updated via NOMIS by syncing the changes from NOMIS to the new service
* Document and plan all work needed to move to a one-way sync

## Consequences

* Two-way sync provides maximum flexibility for rollout strategy and maximum safety for rollback
  * The DPS functionality can be switched to use the new API independently of switching the NOMIS screens off
  * Both sets of switches can be toggled on and off at any time without service disruption
* Two-way sync supports decoupling the data migration from the roll-out itself
  * The full prisoner alerts dataset can be migrated and kept in sync prior to toggling any of the feature switches
  * This synced dataset can be checked for accuracy over a number of weeks to gain confidence that sync is working as intended
* Full prison estate roll-out following a smaller private beta becomes significantly more simple as the prisoner alerts data has already been migrated into the new service and kept in sync since
* Two-way sync supports the current alerts trigger in NOMIS that creates a corresponding case note
