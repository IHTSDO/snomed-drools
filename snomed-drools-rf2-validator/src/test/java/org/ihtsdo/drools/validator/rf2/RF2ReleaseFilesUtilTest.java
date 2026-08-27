package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.otf.snomedboot.ReleaseImportException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RF2ReleaseFilesUtilTest {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void singleSnapshotReleaseCanSkipEffectiveComponentFilter() throws IOException, ReleaseImportException {
		Path release = temporaryFolder.newFolder("release").toPath();
		createFile(release, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260731.txt");
		createFile(release, "Snapshot/Terminology/sct2_Description_Snapshot-en_INT_20260731.txt");

		assertTrue(RF2ReleaseFilesUtil.isSingleSnapshotRelease(Collections.singleton(release.toString())));
	}

	@Test
	public void nestedSnapshotReleasesRetainEffectiveComponentFilter() throws IOException, ReleaseImportException {
		Path release = temporaryFolder.newFolder("release").toPath();
		createFile(release, "international/Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260731.txt");
		createFile(release, "extension/Snapshot/Terminology/sct2_Concept_Snapshot_AU1000036_20260831.txt");

		assertFalse(RF2ReleaseFilesUtil.isSingleSnapshotRelease(Collections.singleton(release.toString())));
	}

	@Test
	public void snapshotAndDeltaRetainEffectiveComponentFilter() throws IOException, ReleaseImportException {
		Path release = temporaryFolder.newFolder("release").toPath();
		createFile(release, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260731.txt");
		createFile(release, "Delta/Terminology/sct2_Description_Delta-en_INT_20260801.txt");

		assertTrue(RF2ReleaseFilesUtil.anyDeltaFilesPresent(Collections.singleton(release.toString())));
		assertFalse(RF2ReleaseFilesUtil.isSingleSnapshotRelease(Collections.singleton(release.toString())));
	}

	@Test
	public void multipleInputRootsRetainEffectiveComponentFilter() throws IOException, ReleaseImportException {
		Path release = temporaryFolder.newFolder("release").toPath();
		Path dependency = temporaryFolder.newFolder("dependency").toPath();
		createFile(release, "Snapshot/Terminology/sct2_Concept_Snapshot_AU1000036_20260831.txt");
		createFile(dependency, "Snapshot/Terminology/sct2_Concept_Snapshot_INT_20260731.txt");

		assertFalse(RF2ReleaseFilesUtil.isSingleSnapshotRelease(Set.of(release.toString(), dependency.toString())));
	}

	private void createFile(Path root, String relativePath) throws IOException {
		Path path = root.resolve(relativePath);
		Files.createDirectories(path.getParent());
		Files.createFile(path);
	}
}
