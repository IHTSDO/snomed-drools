package org.ihtsdo.drools.rulestestrig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.reflect.TypeToken;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class TestUtil {

	public static final Charset UTF8 = Charset.forName("UTF-8");
	private static Gson gson;

	static {
		gson = new GsonBuilder()
				.registerTypeAdapter(Concept.class, new InstanceCreator<Concept>() {
					@Override
					public Concept createInstance(Type type) {
						return new TestConcept();
					}
				})
				.registerTypeAdapter(Description.class, new InstanceCreator<Description>() {
					@Override
					public Description createInstance(Type type) {
						return new TestDescription();
					}
				})
				.registerTypeAdapter(Relationship.class, new InstanceCreator<Relationship>() {
					@Override
					public Relationship createInstance(Type type) {
						return new TestRelationship();
					}
				})
				.create();
	}
	public static final Map<String, List<TestConcept>> loadConceptMap(File jsonFile) throws FileNotFoundException {
		return gson.fromJson(new FileReader(jsonFile), new TypeToken<Map<String, List<TestConcept<TestDescription, TestRelationship>>>>() {}.getType());
	}

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
