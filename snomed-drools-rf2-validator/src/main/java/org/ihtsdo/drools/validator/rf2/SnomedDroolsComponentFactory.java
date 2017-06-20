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

	public SnomedDroolsComponentFactory(SnomedDroolsComponentRepository repository) {
		this.repository = repository;
	}

	@Override
	public void newConceptState(String conceptId, String effectiveTime, String active, String moduleId, String definitionStatusId) {
		// TODO: This information could come from another system or from parsing the FULL RF2 files..
		boolean published = true;
		boolean released = true;
		repository.addConcept(new DroolsConcept(conceptId, isActive(active), moduleId, definitionStatusId, published, released));
	}

	@Override
	public void newDescriptionState(String id, String effectiveTime, String active, String moduleId, String conceptId, String languageCode, String typeId, String term, String caseSignificanceId) {
		boolean published = true;
		boolean released = true;
		repository.addDescription(new DroolsDescription(id, isActive(active), moduleId, conceptId, languageCode, typeId, term, caseSignificanceId, TEXT_DEFINITION.equals(typeId), new HashMap<String, String>(), published, released));
	}

	@Override
	public void newRelationshipState(String id, String effectiveTime, String active, String moduleId, String sourceId, String destinationId, String relationshipGroup, String typeId, String characteristicTypeId, String modifierId) {
		if (!characteristicTypeId.equals(INFERRED_RELATIONSHIP)) {
			boolean published = true;
			boolean released = true;
			repository.addRelationship(new DroolsRelationship(id, isActive(active), moduleId, sourceId, destinationId, Integer.parseInt(relationshipGroup), typeId, characteristicTypeId, published, released));
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
