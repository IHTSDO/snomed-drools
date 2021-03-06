# SNOMED RF2 Drools Engine
An implementation of the SNOMED Drools validation engine which uses RF2 files.

## Usage
Build with maven: `mvn clean install`

Checkout the [snomed-drools-rules](https://github.com/IHTSDO/snomed-drools-rules).

Validate an RF2 Snapshot:

Format: 
```
java -jar target/snomed-drools-rf2-*executable.jar  <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <rf2SnapshotDirectory> <currentEffectiveTime> <includedModules(optional)>
```

Example: 
```
java -jar target/snomed-drools-rf2-*executable.jar  ../snomed-drools-rules common-authoring,int-authoring ../snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20180731T120000Z 20180731
```

## Test Resources
Test resource files are loaded from public cloud storage (AWS S3). 

These resources include a list of known semantic tags, case significant words and a US to GB terms map. Get in touch if you would like a copy of these resources or have ideas about improvements.
