package org.ihtsdo.drools.testrig;

import org.ihtsdo.drools.domain.Description;
import org.json.JSONException;
import org.json.JSONObject;

public class TestRigDescription implements Description {

	private final JSONObject jsonDesc;
	private final String conceptId;

	public TestRigDescription(String conceptId, JSONObject jsonDesc) {
		this.conceptId = conceptId;
		this.jsonDesc = jsonDesc;
	}

	@Override
	public String getId() {
		try {
			return jsonDesc.getString("id");
		} catch (JSONException e) {
			return null;
		}
	}

	@Override
	public String getConceptId() {
		return conceptId;
	}

	@Override
	public String getTerm() {
		try {
			return jsonDesc.getString("term");
		} catch (JSONException e) {
			return null;
		}
	}
}
