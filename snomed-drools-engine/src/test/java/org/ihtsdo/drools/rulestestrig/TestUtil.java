package org.ihtsdo.drools.rulestestrig;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.OntologyAxiom;
import org.ihtsdo.drools.domain.Relationship;
import org.ihtsdo.drools.rulestestrig.domain.TestConcept;
import org.ihtsdo.drools.rulestestrig.domain.TestDescription;
import org.ihtsdo.drools.rulestestrig.domain.TestOntologyAxiom;
import org.ihtsdo.drools.rulestestrig.domain.TestRelationship;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TestUtil {

	public static final Charset UTF8 = Charset.forName("UTF-8");
	private static Gson gson;

	static {
		gson = new GsonBuilder()
				.registerTypeAdapter(Concept.class, (InstanceCreator<Concept>) type -> new TestConcept<TestDescription, TestRelationship>())
				.registerTypeAdapter(Description.class, (InstanceCreator<Description>) type -> new TestDescription())
				.registerTypeAdapter(Relationship.class, (InstanceCreator<Relationship>) type -> new TestRelationship())
				.registerTypeAdapter(OntologyAxiom.class, new TypeAdapter<OntologyAxiom>() {
					@Override
					public OntologyAxiom read(JsonReader jsonReader) throws IOException {
						TestOntologyAxiom ontologyAxiom = new TestOntologyAxiom();
						jsonReader.beginObject();
						while (jsonReader.hasNext()) {
							String name = jsonReader.nextName();
							if (name.equals("active")) {
								ontologyAxiom.setActive(jsonReader.nextBoolean());
							} else if (name.equals("owlExpressionNamedConcepts")) {
								jsonReader.beginArray();
								HashSet<String> concept = new HashSet<>();
								ontologyAxiom.setOwlExpressionNamedConcepts(concept);
								while (jsonReader.hasNext()) {
									concept.add(jsonReader.nextString());
								}
								jsonReader.endArray();
							}
						}
						jsonReader.endObject();
						return ontologyAxiom;
					}

					@Override
					public void write(JsonWriter jsonWriter, OntologyAxiom ontologyAxiom) throws IOException {

					}
				})
				.create();
	}
	
	public static final Map<String, List<TestConcept<TestDescription, TestRelationship>>> loadConceptMap(File jsonFile) throws FileNotFoundException {
		return gson.fromJson(new FileReader(jsonFile), new TypeToken<Map<String, List<TestConcept<TestDescription, TestRelationship>>>>() {}.getType());
	}

	public static final FileFilter DIRECTORY_FILTER = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory() && !file.isHidden();
		}
	};

	public static final FilenameFilter RULE_FILE_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File file, String name) {
			return new File(file, name).isFile() && name.endsWith(".drl");
		}
	};

}
