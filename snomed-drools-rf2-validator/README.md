# SNOMED RF2 Drools Engine
An implementation of the SNOMED Drools validation engine which uses RF2 files.

## Usage
Build with maven: `mvn clean install`

Checkout the [snomed-drools-rules](https://github.com/IHTSDO/snomed-drools-rules).

### Validate an RF2 Snapshot:
Using a directory containing extracted snapshot files. To validate an extension extract the dependant International files and the extension files into one or more directories. 
The parameter `extractedRF2FilesDirectories` is a comma separated list of directory paths.

#### Format: 
```
java -jar target/snomed-drools-rf2-*executable.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <extractedRF2FilesDirectories> <currentEffectiveTime> <includedModules(optional)>
```

#### Example validating a SNOMED CT Edition: 
```
java -jar target/snomed-drools-rf2-*executable.jar ../snomed-drools-rules common-authoring,int-authoring ../snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20210131T120000Z 20210131
```
... where `../snomed-releases` is the path to your SNOMED CT release files.

#### Validating an Extension:
When validating an extension we provide the extracted International files and the extracted extension files. In this example we also filter the validation issues 
reported by module. 
```
java -jar target/snomed-drools-rf2-*executable.jar ../snomed-drools-rules common-authoring,be-authoring ../snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20210131T120000Z,../snomed-releases/SnomedCT_BelgiumExtensionRF2_PRODUCTION_20210315T120000Z 20210315 11000172109
```

### Validate the Current Authoring Cycle:
When validating the content of the current authoring cycle there may be no snapshot available. In this scenario a delta export can be combined with the previous release snapshot 
to create a complete set of components for validation. Works for extensions too.

#### Format:
```
java -jar target/snomed-drools-rf2-*executable.jar <snomedDroolsRulesPath> <assertionGroup1,assertionGroup2,etc> <extractedRF2FilesDirectories> <currentEffectiveTime> <includedModules(optional)> <previousReleaseRf2Directories(optional)>
```
#### Example validating a SNOMED CT Edition:
```
java -jar target/snomed-drools-rf2-*executable.jar ../snomed-drools-rules common-authoring,int-authoring ../snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20210131T120000Z,../snomed-releases/rf2-export_MAIN 20210131 ../snomed-releases/SnomedCT_InternationalRF2_PRODUCTION_20210131T120000Z
```
In this example the directory of previous release files has been given twice. Once in the `extractedRF2FilesDirectories` parameter, to be combined with the delta, and once in the
`previousReleaseRf2Directories` parameter, so the application can establish which components have previously been published.


## Test Resources
Test resource files are loaded from public cloud storage (AWS S3). 

These resources include a list of known semantic tags, case significant words and a US to GB terms map. Get in touch if you would like a copy of these resources or have ideas about improvements.
