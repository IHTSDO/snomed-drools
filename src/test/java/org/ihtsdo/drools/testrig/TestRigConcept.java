package org.ihtsdo.drools.testrig;

import org.ihtsdo.drools.domain.Concept;
import org.ihtsdo.drools.domain.Description;
import org.ihtsdo.drools.domain.Relationship;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestRigConcept implements Concept {

	private final JSONObject concept;
	private final List<Description> descriptions;
	private final List<Relationship> relationships;

	public TestRigConcept(JSONObject jsonConcept) {
		this.concept = jsonConcept;

		descriptions = new ArrayList<>();
		final JSONArray jsonDescriptions;
		try {
			jsonDescriptions = jsonConcept.getJSONArray("descriptions");
			for (int i = 0; i < jsonDescriptions.length(); i++) {
				final JSONObject jsonDesc = jsonDescriptions.getJSONObject(i);
				descriptions.add(new TestRigDescription(getId(), jsonDesc));
			}
		} catch (JSONException e) {
			// No action required.
		}

		relationships = new ArrayList<>();
	}

	@Override
	public String getId() {
		try {
			return concept.getString("id");
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public Collection<Description> getDescriptions() {
		return descriptions;
	}

	@Override
	public Collection<Relationship> getRelationships() {
		return relationships;
	}
}
