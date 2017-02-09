# Snomed Drools Engine
A SNOMED CT Concept Validation Engine using Drools (Business Rules Engine).

This component is used in the SNOMED International Authoring Platform to validate the parts of a concept which have changed during authoring. We have made a concious effort to write this project in a way that the code can be reused in any SNOMED authoring tool.

To integrate this tool with your system please implempent these interfaces:
- [Domain object inferfaces](https://github.com/IHTSDO/snomed-drools/tree/master/src/main/java/org/ihtsdo/drools/domain)
- [Service inferfaces](https://github.com/IHTSDO/snomed-drools/tree/master/src/main/java/org/ihtsdo/drools/service)

These interfaces are implemented in the Authoring Terminology Server. We plan to also implement them in the Release Validation Framework to reuse the rules and reduce rule maintenance.

The rules for this engine are maintained seperately, the standard set can be found here - https://github.com/IHTSDO/snomed-drools-rules
