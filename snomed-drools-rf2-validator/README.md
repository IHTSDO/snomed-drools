# SNOMED RF2 Drools Engine
An implementation of the SNOMED Drools validation engine which uses RF2 files.

## Usage
Build with maven: `mvn clean install`

Checkout the [snomed-drools-rules](https://github.com/IHTSDO/snomed-drools-rules).

### Validate an RF2 Snapshot:
Using a directory containing extracted snapshot files. To validate an extension extract the dependant International files and the extension files into one or more directories. 
The parameter `extractedRF2FilesDirectories` is a comma separated list of directory paths.

Format: 
```
java -jar target/snomed-drools-rf2-*executable.jar  <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <extractedRF2FilesDirectories> <currentEffectiveTime> <includedModules(optional)>
```

Example: 
```
java -jar target/snomed-drools-rf2-*executable.jar  ../snomed-drools-rules common-authoring,int-authoring ../snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20180731T120000Z 20180731
```

### Validate an RF2 Snapshot and Delta:
Using a snapshot of the previous release (as above) and a delta of the new or current release cycle. Also works for extensions.

Format:
```
java -jar target/snomed-drools-rf2-*executable.jar  <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <extractedRF2FilesDirectory> <currentEffectiveTime> <includedModules(optional)>
```

## Test Resources
Test resource files are loaded from public cloud storage (AWS S3). 

These resources include a list of known semantic tags, case significant words and a US to GB terms map. Get in touch if you would like a copy of these resources or have ideas about improvements.
