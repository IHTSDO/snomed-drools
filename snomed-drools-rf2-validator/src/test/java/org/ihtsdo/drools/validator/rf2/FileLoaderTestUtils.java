package org.ihtsdo.drools.validator.rf2;

import com.google.gson.Gson;
import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.ihtsdo.otf.snomedboot.ReleaseImporter;
import org.ihtsdo.otf.snomedboot.factory.LoadingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.traversal.TreeWalker;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileLoaderTestUtils {

	private static Logger logger = LoggerFactory.getLogger(FileLoaderTestUtils.class);

	public static String copyRF2RemovingComments(String rf2Directory) throws IOException {
		final Path tempDirectory = Files.createTempDirectory(UUID.randomUUID().toString());
		final Set<Path> rf2FilesToCopy = Files.walk(Paths.get(rf2Directory))
				.filter(path -> path.toFile().isFile() && path.toFile().getName().endsWith(".txt"))
				.collect(Collectors.toSet());
		logger.info("Copying files {}", rf2FilesToCopy);
		for (Path fileToCopy : rf2FilesToCopy) {
			final Path destination = Paths.get(tempDirectory.toString(), fileToCopy.getFileName().toString());
			try (final BufferedReader reader = Files.newBufferedReader(fileToCopy);
				 final BufferedWriter writer = Files.newBufferedWriter(destination)) {

				logger.info("Copying {} to temp file {}", fileToCopy, destination);

				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("#") && !line.isEmpty()) {
						writer.write(line);
						writer.newLine();
					}
				}
			}
		}
		return tempDirectory.toFile().getAbsolutePath();
	}

    public static <T> T fileToObject(String filePath, Class<T> clazz) throws FileNotFoundException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(filePath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(url.getFile()));
        Gson gson = new Gson();
        return gson.fromJson(bufferedReader, clazz);
    }

}
