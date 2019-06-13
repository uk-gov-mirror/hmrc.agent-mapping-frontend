# agent-mapping-frontend

[![Build Status](https://travis-ci.org/hmrc/agent-mapping-frontend.svg)](https://travis-ci.org/hmrc/agent-mapping-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-mapping-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agent-mapping-frontend/_latestVersion)

This is a web frontend service providing a journey for agents to capture references to their non-MTD relationships with their clients.

### Purpose

Agents act on behalf of one or more clients. To do so they require a valid relationship to be in place.

As agents move to MDTP, the intention where possible is that their Agent/Client relationships existing within the Heads of Duty (HoD) are honoured.

This mapping journey will capture the Agent's old reference/identifier used by each HoD and associate it with the Agent's new Agent Reference Number (ARN).
After the mapping journey has been completed, the association will subsequently allow the old relationship within the HoD to be honoured.

#### Mapping journey

Within the Mapping journey an Agent will log in with GG credential.
If that credential has the enrolments and subsequently the identifiers that we are looking for then they will be prompted to enter their ARN and known fact(s). The service then stores the ARN and the identifiers within a mapping store.

As an example, CESA would be the Head of Duty for Self Assessment relationships.
This mapping journey will capture the CESA Agent Ref and associate it with the Agent's ARN.

### Running the tests

    sbt test it:test

### Running the app locally

    sm --start AGENT_ONBOARDING -r
    sm --stop AGENT_MAPPING_FRONTEND
    sbt run

It should then be listening on port 9438.

The start page of the journey begins at:

    browse http://localhost:9438/agent-mapping/start
    
    
    
