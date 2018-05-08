package org.ihtsdo.drools.validator.rf2;

import com.google.gson.Gson;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

public class FileLoaderTestUtils {


    public static void loadReleaseFile(String releaseFilePath, SnomedDroolsComponentRepository repository) {
        ReleaseImporter importer = new ReleaseImporter();
        LoadingProfile loadingProfile = LoadingProfile.complete;
        loadingProfile.getIncludedReferenceSetFilenamePatterns().add(".*_cRefset_Language.*");
        try {
            importer.loadSnapshotReleaseFiles(new FileInputStream(releaseFilePath), loadingProfile, new SnomedDroolsComponentFactory(repository, ""));
        } catch (ReleaseImportException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static <T> T fileToObject(String filePath, Class<T> clazz) throws FileNotFoundException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(filePath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(url.getFile()));
        Gson gson = new Gson();
        return gson.fromJson(bufferedReader, clazz);
    }


}
