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

## Running the RF2 Validator
The `snomed-drools-rf2-validator` module builds an executable jar which validates extracted RF2 files and writes the results to `validation-report-<timestamp>.txt` in the working directory.

```
java -jar snomed-drools-rf2-validator-<version>-executable.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <extractedRF2FilesDirectories> <currentEffectiveTime> <includedModules(optional)> <previousReleaseRf2Directories(optional)>
```

- `currentEffectiveTime` uses the format `yyyymmdd`.
- `extractedRF2FilesDirectories` and `previousReleaseRf2Directories` are comma separated directory paths.
- `includedModules` is a comma separated list of module ids.

## Configuration
The engine and the RF2 Validator read the following JVM system properties.
They are read with `System.getProperty`, so they must be passed as `-D` options to the JVM of the application using this library (e.g. Snowstorm, RVF or the RF2 Validator jar). Spring `application.properties` entries are not picked up.

| Property | Module | Default | Description |
|---|---|---|---|
| `snomed.drools.validation.threads` | `snomed-drools-engine` | Number of available processors | Number of workers used to validate concepts in parallel. Must be a positive integer. Memory grows with this value because each worker has its own Drools session per assertion group. |
| `snomed.drools.parallelLoading` | `snomed-drools-rf2-validator` | `true` | Read RF2 files concurrently. Set to `false` only to rule out the loader while diagnosing a problem. |

Example:
```
java -Dsnomed.drools.validation.threads=8 -jar snomed-drools-rf2-validator-<version>-executable.jar ...
```

The worker count is read when a `RuleExecutor` is created. Snowstorm creates one at startup and on rule reload, RVF creates one per validation run.
It can also be set in code with `RuleExecutor.setValidationThreads(int)`.

## The Assertion Rules
The rules for this engine are maintained separately in the [SNOMED Drools Rules project](https://github.com/IHTSDO/snomed-drools-rules).
