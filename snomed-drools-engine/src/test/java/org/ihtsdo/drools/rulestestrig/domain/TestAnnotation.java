package org.ihtsdo.drools.rulestestrig.domain;

import org.ihtsdo.drools.domain.Annotation;

public class TestAnnotation implements Annotation, TestComponent{
    private String id;
    private String effectiveTime;
    private boolean active;
    private boolean published;
    private boolean released;
    private String moduleId;
    private String conceptId;
    private String languageDialectCode;
    private String typeId;
    private String value;

    public TestAnnotation() {
        active = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(String effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    @Override
    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    public void setModuleId(String moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    @Override
    public String getLanguageDialectCode() {
        return languageDialectCode;
    }

    public void setLanguageDialectCode(String languageDialectCode) {
        this.languageDialectCode = languageDialectCode;
    }

    @Override
    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TestAnnotation{" +
                "id='" + id + '\'' +
                ", effectiveTime='" + effectiveTime + '\'' +
                ", active=" + active +
                ", published=" + published +
                ", released=" + released +
                ", moduleId='" + moduleId + '\'' +
                ", conceptId='" + conceptId + '\'' +
                ", languageDialectCode='" + languageDialectCode + '\'' +
                ", typeId='" + typeId + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
