# ADR002: Migrate full NOMIS case notes data set across all bookings

## Status

Approved by @Alice Noakes and @Jess Morrow as Service Owners and @Richard Adams as Principal Technical Architect

## Context

NOMIS associates all data with offender records via their bookings. By default, only the data associated with the current booking is displayed in DPS and NOMIS although in the case of NOMIS, users can switch to historic bookings.

Certain data domains, e.g. Alerts simulate the direct association of data to a person by copying all the data from the previous booking to the current booking. Case notes is not one of these domains meaning that when a person enters prison with a new booking, they have no case notes against that booking. Viewing that person's prisoner profile in DPS and their case notes in NOMIS would currently show no case notes except for the automatically generated case notes associated with the copying of their alerts.

The case notes associated with their previous bookings are still present in the NOMIS database but can only be viewed if a user switches to a historic booking in NOMIS and cannot currently be viewed at all in DPS. This latter limitation has proven an issue for prison staff.

## Decision

The Move and Improve team in collaboration with Syscon will migrate all case notes across all bookings. These migrated case notes will be associated directly with a person via their prison(er) number and no booking information will be retained.

The Offender Case Notes API will return case notes by default in decending order of creation and will be paginated. It will support filtering by date range.

## Consequences

* All case notes data will be available in the Offender Case Notes API's database allowing an eventual decommissioning of the NOMIS case notes data without risking data loss
* DPS users will be able to view the full history of a person's case notes
* DPS users will be able to sort and filter the data for their specific need