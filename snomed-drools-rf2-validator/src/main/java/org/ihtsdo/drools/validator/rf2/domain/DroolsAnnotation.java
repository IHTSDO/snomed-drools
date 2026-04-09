package org.ihtsdo.drools.validator.rf2.domain;

import org.ihtsdo.drools.domain.Annotation;

public class DroolsAnnotation extends DroolsComponent implements Annotation {

    private final String conceptId;
    private final String languageDialectCode;
    private final String typeId;
    private final String value;

    public DroolsAnnotation(String id, String effectiveTime, boolean active, String moduleId, boolean published, boolean released, String conceptId, String languageDialectCode, String typeId, String value) {
        super(id, effectiveTime, active, moduleId, published, released);
        this.conceptId = conceptId;
        this.languageDialectCode = languageDialectCode;
        this.typeId = typeId;
        this.value = value;
    }

    @Override
    public String getConceptId() {
        return conceptId;
    }

    @Override
    public String getTypeId() {
        return typeId;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLanguageDialectCode() {
        return languageDialectCode;
    }
}
