package org.ihtsdo.drools.helper;

import org.ihtsdo.drools.domain.ComponentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class IdentifierHelper {

    public static final Pattern SCTID_PATTERN = Pattern.compile("\\d{6,18}");

    private static final String PARTITION_PART2_CONCEPT = "0";
    private static final String PARTITION_PART2_DESCRIPTION = "1";
    private static final String PARTITION_PART2_RELATIONSHIP = "2";


    private final static Logger logger = LoggerFactory.getLogger(IdentifierHelper.class);

    public static boolean isConceptId(String sctid) {
        return sctid != null && SCTID_PATTERN.matcher(sctid).matches() && PARTITION_PART2_CONCEPT.equals(getPartitionIdPart(sctid));
    }

    public static boolean isDescriptionId(String sctid) {
        return sctid != null && SCTID_PATTERN.matcher(sctid).matches() && PARTITION_PART2_DESCRIPTION.equals(getPartitionIdPart(sctid));
    }

    public static boolean isRelationshipId(String sctid) {
        return sctid != null && SCTID_PATTERN.matcher(sctid).matches() && PARTITION_PART2_RELATIONSHIP.equals(getPartitionIdPart(sctid));
    }

    public static boolean isValidId(String sctid, ComponentType componentType) {
        if (StringUtils.isEmpty(sctid) || sctid.contains("-")) {
            return true;
        }

        boolean isValid = false;
        try {
            if (VerhoeffCheck.validateLastChecksumDigit(sctid) && componentType != null) {
                switch (componentType) {
                    case Concept:
                        isValid = isConceptId(sctid);
                        break;
                    case Description:
                        isValid = isDescriptionId(sctid);
                        break;
                    case Relationship:
                        isValid = isRelationshipId(sctid);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return isValid;
    }

    private static String getPartitionIdPart(String sctid) {
        if (!StringUtils.isEmpty(sctid) && sctid.length() > 4) {
            return sctid.substring(sctid.length() - 2, sctid.length() - 1);
        }
        return null;
    }
}
