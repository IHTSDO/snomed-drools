package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FileLoaderTestUtils {


    public static void loadReleaseFile(String releaseFilePath, SnomedDroolsComponentRepository repository) {
        ReleaseImporter importer = new ReleaseImporter();
        LoadingProfile loadingProfile = LoadingProfile.complete;
        loadingProfile.getIncludedReferenceSetFilenamePatterns().add(".*_cRefset_Language.*");
        try {
            importer.loadSnapshotReleaseFiles(new FileInputStream(releaseFilePath), loadingProfile, new SnomedDroolsComponentFactory(repository));
        } catch (ReleaseImportException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }


}
