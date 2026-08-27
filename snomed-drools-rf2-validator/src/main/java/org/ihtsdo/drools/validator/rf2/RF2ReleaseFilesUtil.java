package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Pattern;

public class RF2ReleaseFilesUtil {

    private static final Pattern CONCEPT_SNAPSHOT_FILE = Pattern.compile("x?(sct|rel)2_Concept_[^_]*Snapshot_.*\\.txt");
    private static final Pattern RF2_DELTA_FILE = Pattern.compile("x?(sct|der|rel)2_.*Delta[^_]*_.*\\.txt");

    private RF2ReleaseFilesUtil() {}

    public static boolean anyDeltaFilesPresent(Set<String> extractedRF2FilesDirectories) throws ReleaseImportException {
        for (String extractedRF2FilesDirectory : extractedRF2FilesDirectories) {
            try (final Stream<Path> pathStream = Files.find(new File(extractedRF2FilesDirectory).toPath(), 50,
                    (path, basicFileAttributes) -> RF2_DELTA_FILE.matcher(path.toFile().getName()).matches())) {
                if (pathStream.findFirst().isPresent()) {
                    return true;
                }
            } catch (IOException e) {
                throw new ReleaseImportException("Error while searching input files.", e);
            }
        }
        return false;
    }

    static boolean isSingleSnapshotRelease(Set<String> extractedRF2FilesDirectories) throws ReleaseImportException {
        if (extractedRF2FilesDirectories.size() != 1) {
            return false;
        }

        String extractedRF2FilesDirectory = extractedRF2FilesDirectories.iterator().next();
        int conceptSnapshotFiles = 0;
        try (Stream<Path> pathStream = Files.find(new File(extractedRF2FilesDirectory).toPath(), 50,
                (path, basicFileAttributes) -> basicFileAttributes.isRegularFile())) {
            Iterator<Path> paths = pathStream.iterator();
            while (paths.hasNext()) {
                String filename = paths.next().toFile().getName();
                if (RF2_DELTA_FILE.matcher(filename).matches()) {
                    return false;
                }
                if (CONCEPT_SNAPSHOT_FILE.matcher(filename).matches() && ++conceptSnapshotFiles > 1) {
                    return false;
                }
            }
        } catch (IOException e) {
            throw new ReleaseImportException("Error while searching input files.", e);
        }
        return conceptSnapshotFiles == 1;
    }
}
