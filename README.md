# SNOMED Drools Engine
A SNOMED CT Concept Validation Engine using [Drools (Business Rules Engine)](http://drools.org).

This library is used in the SNOMED International Authoring Platform to validate the parts of a concept which have changed during authoring.

## The Engine
The `snomed-drools-engine` module is responsible for loading sets of assertions and other test resources then executing those assertions on SNOMED CT content as requested.
The services within the engine, which are used to access SNOMED CT content, are abstract interfaces.
No implementation for accessing content is provided within the `snomed-drools-engine` module so it can not perform any validation on its own.
The engine has been written in this way intentionally to allow the same assertions to be run in a wide range of implementations.

The interfaces can be found here:
- [Domain object interfaces](https://github.com/IHTSDO/snomed-drools/tree/master/src/main/java/org/ihtsdo/drools/domain)
- [Service interfaces](https://github.com/IHTSDO/snomed-drools/tree/master/src/main/java/org/ihtsdo/drools/service)

## Axiom validation
For each OWL axiom to be validated the OWL expression should be deserialized and converted to a Relationship domain object. 

The same SNOMED Drools Rules apply to axiom fragments as to relationships. 

## Content Access Implementations
The following implementations use the SNOMED Drools Engine and implement the interfaces above to give the engine access to SNOMED CT content:

* [Snowstorm Terminology Server](https://github.com/IHTSDO/snowstorm) (in production)
* [SNOMED Drools RF2 Validator](https://github.com/IHTSDO/snomed-drools/tree/master/snomed-drools-rf2-validator)
  * [SNOMED Release Validation Framework](https://github.com/IHTSDO/release-validation-framework) uses the RF2 Validator (in production)
* [Snow Owl Terminology Server](https://github.com/IHTSDO/snow-owl) (now retired)

## The Assertion Rules
The rules for this engine are maintained separately in the [SNOMED Drools Rules project](https://github.com/IHTSDO/snomed-drools-rules).
