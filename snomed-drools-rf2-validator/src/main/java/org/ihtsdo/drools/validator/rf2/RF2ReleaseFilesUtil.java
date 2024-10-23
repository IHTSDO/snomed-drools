package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public class RF2ReleaseFilesUtil {

    private RF2ReleaseFilesUtil() {}

    public static boolean anyDeltaFilesPresent(Set<String> extractedRF2FilesDirectories) throws ReleaseImportException {
        for (String extractedRF2FilesDirectory : extractedRF2FilesDirectories) {
            try (final Stream<Path> pathStream = Files.find(new File(extractedRF2FilesDirectory).toPath(), 50,
                    (path, basicFileAttributes) -> path.toFile().getName().matches("x?(sct|rel)2_Concept_[^_]*Delta_.*.txt"))) {
                if (pathStream.findFirst().isPresent()) {
                    return true;
                }
            } catch (IOException e) {
                throw new ReleaseImportException("Error while searching input files.", e);
            }
        }
        return false;
    }
}
