package org.ihtsdo.drools.testrig;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.charset.Charset;

public class TestUtil {

	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};

	public static final FilenameFilter RULE_FILE_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File file, String name) {
			return new File(file, name).isFile() && name.endsWith(".drl");
		}
	};

}
