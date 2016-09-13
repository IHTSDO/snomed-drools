package org.ihtsdo.drools.domain;

import java.util.HashSet;
import java.util.Set;

public class Constants {

	public static final String FSN = "900000000000003001";
	public static final String SYNONYM = "900000000000013009";
	public static final String TEXT_DEFINITION = "900000000000550004";
	public static final String US_EN_LANG_REFSET = "900000000000509007";
	public static final String GB_EN_LANG_REFSET = "900000000000508004";
	public static final String ACCEPTABILITY_PREFERRED = "900000000000548007";
	public static final String ACCEPTABILITY_ACCEPTABLE = "900000000000549004";
	public static final String ENTIRE_TERM_CASE_SENSITIVE = "900000000000017005";
	public static final String ENTIRE_TERM_CASE_INSENSITIVE = "900000000000448009";
	public static final String ONLY_INITIAL_CHARACTER_CASE_INSENSITIVE = "900000000000020002";
	public static final String IS_A = "116680003";
	public static final String PRIMITIVE = "900000000000074008";
	public static final String DEFINED = "900000000000073002";
	public static final String INFERRED_RELATIONSHIP = "900000000000011006";
	public static final String STATED_RELATIONSHIP = "900000000000010007";
	
	public static final Set<String> SEMANTIC_TAGS = new HashSet<>();
	static {
		SEMANTIC_TAGS.add("assessment scale");
		SEMANTIC_TAGS.add("attribute");
		SEMANTIC_TAGS.add("body structure");
		SEMANTIC_TAGS.add("cell structure");
		SEMANTIC_TAGS.add("cell");
		SEMANTIC_TAGS.add("core metadata concept");
		SEMANTIC_TAGS.add("disorder");
		SEMANTIC_TAGS.add("environment / location");
		SEMANTIC_TAGS.add("environment");
		SEMANTIC_TAGS.add("ethnic group");
		SEMANTIC_TAGS.add("event");
		SEMANTIC_TAGS.add("finding");
		SEMANTIC_TAGS.add("foundation metadata concept");
		SEMANTIC_TAGS.add("geographic location");
		SEMANTIC_TAGS.add("inactive concept");
		SEMANTIC_TAGS.add("life style");
		SEMANTIC_TAGS.add("link assertion");
		SEMANTIC_TAGS.add("linkage concept");
		SEMANTIC_TAGS.add("metadata");
		SEMANTIC_TAGS.add("morphologic abnormality");
		SEMANTIC_TAGS.add("namespace concept");
		SEMANTIC_TAGS.add("navigational concept");
		SEMANTIC_TAGS.add("observable entity");
		SEMANTIC_TAGS.add("occupation");
		SEMANTIC_TAGS.add("organism");
		SEMANTIC_TAGS.add("person");
		SEMANTIC_TAGS.add("physical force");
		SEMANTIC_TAGS.add("physical object");
		SEMANTIC_TAGS.add("procedure");
		SEMANTIC_TAGS.add("product");
		SEMANTIC_TAGS.add("qualifier value");
		SEMANTIC_TAGS.add("racial group");
		SEMANTIC_TAGS.add("record artifact");
		SEMANTIC_TAGS.add("regime/therapy");
		SEMANTIC_TAGS.add("religion/philosophy");
		SEMANTIC_TAGS.add("situation");
		SEMANTIC_TAGS.add("social concept");
		SEMANTIC_TAGS.add("special concept");
		SEMANTIC_TAGS.add("specimen");
		SEMANTIC_TAGS.add("staging scale");
		SEMANTIC_TAGS.add("substance");
		SEMANTIC_TAGS.add("tumor staging");
	}


}
