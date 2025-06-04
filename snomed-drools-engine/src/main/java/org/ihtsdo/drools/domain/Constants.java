package org.ihtsdo.drools.domain;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

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
	public static final String ROOT_CONCEPT = "138875005";
	public static final String LANGUAGE_TYPE_CONCEPT = "900000000000506000";
	public static final String REFSET_POSSIBLY_EQUIVALENT_TO_ASSOCIATION = "900000000000523009";
	public static final String REFSET_MOVED_TO_ASSOCIATION = "900000000000524003";
	public static final String REFSET_MOVED_FROM_ASSOCIATION = "900000000000525002";
	public static final String REFSET_REPLACED_BY_ASSOCIATION = "900000000000526001";
	public static final String REFSET_SAME_AS_ASSOCIATION = "900000000000527005";
	public static final String REFSET_WAS_A_ASSOCIATION = "900000000000528000";
	public static final String REFSET_SIMILAR_TO_ASSOCIATION = "900000000000529008";
	public static final String REFSET_ALTERNATIVE_ASSOCIATION = "900000000000530003";
	public static final String REFSET_REFERS_TO_ASSOCIATION = "900000000000531004";
	public static final String REFSET_PARTIALLY_EQUIVALENT_TO_ASSOCIATION = "1186924009";
	public static final String REFSET_POSSIBLY_REPLACED_BY_ASSOCIATION = "1186921001";

	public static final String ERROR_COMPONENT_RULE_ID = "component-loading-error";

	public static final String WARNING_COMPONENT_RULE_ID = "component-loading-warning";

	public static final BiMap <String, String> historicalAssociationNames = new ImmutableBiMap.Builder<String, String>()
			.put(REFSET_POSSIBLY_EQUIVALENT_TO_ASSOCIATION, "POSSIBLY_EQUIVALENT_TO")
			.put(REFSET_MOVED_TO_ASSOCIATION, "MOVED_TO")
			.put(REFSET_MOVED_FROM_ASSOCIATION, "MOVED_FROM")
			.put(REFSET_REPLACED_BY_ASSOCIATION, "REPLACED_BY")
			.put(REFSET_SAME_AS_ASSOCIATION, "SAME_AS")
			.put(REFSET_WAS_A_ASSOCIATION, "WAS_A")
			.put(REFSET_SIMILAR_TO_ASSOCIATION, "SIMILAR_TO")
			.put(REFSET_ALTERNATIVE_ASSOCIATION, "ALTERNATIVE")
			.put(REFSET_REFERS_TO_ASSOCIATION, "REFERS_TO")
			.put(REFSET_PARTIALLY_EQUIVALENT_TO_ASSOCIATION, "PARTIALLY_EQUIVALENT_TO")
			.put(REFSET_POSSIBLY_REPLACED_BY_ASSOCIATION, "POSSIBLY_REPLACED_BY")
			.build();
}
