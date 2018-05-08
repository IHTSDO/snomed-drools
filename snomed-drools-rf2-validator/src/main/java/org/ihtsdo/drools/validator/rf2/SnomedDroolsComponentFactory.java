package org.ihtsdo.drools.validator.rf2;

import org.ihtsdo.drools.validator.rf2.domain.DroolsConcept;
import org.ihtsdo.drools.validator.rf2.domain.DroolsDescription;
import org.ihtsdo.drools.validator.rf2.domain.DroolsRelationship;
import org.ihtsdo.otf.snomedboot.factory.ImpotentComponentFactory;

import java.util.HashMap;

public class SnomedDroolsComponentFactory extends ImpotentComponentFactory {

	private static final String TEXT_DEFINITION = "900000000000550004";
	public static final String INFERRED_RELATIONSHIP = "900000000000011006";
	private final SnomedDroolsComponentRepository repository;
	public static final boolean PUBLISHED = false;
	public static final boolean RELEASED = false;

	public SnomedDroolsComponentFactory(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		repository.addConcept(new DroolsConcept(conceptId, isActive(active), moduleId, definitionStatusId, PUBLISHED, RELEASED));
	}

	@Override
	public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		repository.addDescription(new DroolsDescription(id, isActive(active), moduleId, conceptId, languageCode, typeId, term, caseSignificanceId, TEXT_DEFINITION.equals(typeId), PUBLISHED, RELEASED));
	}

	@Override
	public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		if (!characteristicTypeId.equals(INFERRED_RELATIONSHIP)) {
			repository.addRelationship(new DroolsRelationship(id, isActive(active), moduleId, sourceId, destinationId, Integer.parseInt(relationshipGroup), typeId, characteristicTypeId, PUBLISHED, RELEASED));
		}
	}

	@Override
	public void newReferenceSetMemberState(String[] fieldNames, String id, String effectiveTime, String active, String moduleId, String refsetId, String referencedComponentId, String... otherValues) {
		if (isActive(active) && fieldNames.length == 7 && fieldNames[6].equals("acceptabilityId")) {
			// Language reference set
			String acceptabilityId = otherValues[0];
			repository.addLanguageReferenceSetMember(id, referencedComponentId, refsetId, acceptabilityId);
		}
	}

	private boolean isActive(String active) {
		return "1".equals(active);
	}
}
