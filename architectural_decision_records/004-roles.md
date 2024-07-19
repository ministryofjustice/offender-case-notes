# ADR004: Add new roles in line with agreed naming convention and remove reliance on DPS roles and user tokens

## Status

Proposed

## Context

The Offender Case Notes API predates the definition of authentication flows, token usage and role naming agreed in RFC 4. As a result, the API was written with the expectation that a user token, a token obtained via the `authorization_code` flow, would be used to authenticate and authorize API calls.

The API also does not require any roles for most endpoints. The roles were instead used to restrict access to the sensitive case notes where a sensitive case note is defined as using a DPS case note sub-type where the `sensitive` property is `true`.

The Offender Case Notes API acts as a proxy for the Prison API. It is the system of record for DPS case notes, those case notes stored only in the Offender Case Notes database of which secure case notes are a subset, and it calls the Prison API for NOMIS case notes. The API combines these two data sets in memory and it is this combining that is affected by the caller's roles.

There are four DPS user roles used by the API:

1. `POM`
2. `VIEW_SENSITIVE_CASE_NOTES`
3. `ADD_SENSITIVE_CASE_NOTES`
4. `DELETE_SENSITIVE_CASE_NOTES`

A client with the `POM`, `VIEW_SENSITIVE_CASE_NOTES` or `ADD_SENSITIVE_CASE_NOTES` roles will:

* Include sensitive case notes when retrieving case notes
* Include sensitive case note sub-types when retrieving case note types and sub-types

A client with the `POM` or `ADD_SENSITIVE_CASE_NOTES` roles can additionally:

* Create case notes using DPS case note sub-types where the `restrictedUse` property is `true`
* Amend a case note that uses DPS case note sub-types where the `restrictedUse` property is `true`

A client with the `DELETE_SENSITIVE_CASE_NOTES` role can additionally:

* Delete **any** DPS case note regardless of the associated case note sub-type's `sensitive` and `restrictedUse` property values
* Delete an amendment of **any** DPS case note regardless of the associated case note sub-type's `sensitive` and `restrictedUse` property values

The key point to note is that these roles are DPS user roles normally assigned to user accounts only. This meant that the majority of Offender Case Notes API endpoints had to be called with a user token to correctly apply the sensitive and restricted use logic.

The Prisoner Profile got around this by getting the `ADD_SENSITIVE_CASE_NOTES` role added to their UI's API client. They then call the API using a system token obtained via the `client_credentials` flow which contains that role. This means that all Prisoner Profile calls would include sensitive case notes and so an additional `includeSensitive` request parameter was added to the API which is set to `true` only if the user has one of the `POM`, `VIEW_SENSITIVE_CASE_NOTES` or `ADD_SENSITIVE_CASE_NOTES` roles.

## Decision

The Move and Improve team will add two new service roles in line with the standard naming convention

* PRISONER_CASE_NOTES__RO
* PRISONER_CASE_NOTES__RW

### Request parameter

All endpoints that return case notes or case note types and sub-types would have an additional `includeSensitive` request parameter. If a client with one of the sensitive roles supplies `includeSensitive=true` in the request, the API logic will return sensitive and restricted case notes and sub-types in addition to the non sensitive ones.

The `includeSensitive` parameter is already present in the [get case notes endpoint](https://dev.offender-case-notes.service.justice.gov.uk/swagger-ui/index.html#/case-notes/getCaseNotes) and is being correctly populated by the Prisoner Profile. This change would therefore be limited to adding the parameter to the [get types and sub-types endpoint](https://dev.offender-case-notes.service.justice.gov.uk/swagger-ui/index.html#/case-notes/getCaseNoteTypes).

### Existing roles

The existing roles used in the Offender Case Notes API would be retained for compatibility with existing clients until such time as all clients have moved over to the new roles.

Until the existing roles have been decommissioned, the `includeSensitive` request parameter will only be used when the client has one of the new roles. The presence of a new role would indicate that they have switched from passing the user token to using a client token and are therefore aware of the new request parameter.

## Rationale

* RFC 4 states that all API clients should use a system token obtained via the `client_credentials` flow and this decision supports that
* There is already an implicit trust relationship between API and client when passing the jwt `subject` and `username` claims

## Consequences

* The Offender Case Notes API can move to securing every endpoint with a system role therefore rejecting user tokens
* The API client becomes responsible for deciding when it is appropriate to view and manage sensitive and restricted case notes based on their business logic
* DPS user roles no longer need assigning to system clients