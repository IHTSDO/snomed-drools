package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.ComponentType;
import org.junit.Assert;
import org.junit.Test;

public class IdentifierHelperTest {

    @Test
    public void testInvalidSctid() {
        Assert.assertFalse(IdentifierHelper.isValidId("test", ComponentType.Concept));
        Assert.assertFalse(IdentifierHelper.isValidId("test", ComponentType.Description));
        Assert.assertFalse(IdentifierHelper.isValidId("test", ComponentType.Relationship));
    }

    @Test
    public void testValidSctid() {
        Assert.assertTrue(IdentifierHelper.isValidId("1182278001", ComponentType.Concept));
        Assert.assertTrue(IdentifierHelper.isValidId("4656471015", ComponentType.Description));
        Assert.assertTrue(IdentifierHelper.isValidId("3267539029", ComponentType.Relationship));
    }
}
