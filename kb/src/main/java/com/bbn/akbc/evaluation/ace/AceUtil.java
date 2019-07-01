package com.bbn.akbc.evaluation.ace;

import com.google.common.collect.ImmutableMap;

public class AceUtil {
	static ImmutableMap<String,String> aceSubtype2type = ImmutableMap.<String, String>builder()
			.put("Located", "Physical")  //
			.put("Near", "Physical")  //

			.put("Geographical", "Part-whole")
			.put("Subsidiary", "Part-whole")

			.put("Business", "Personal-Social")
			.put("Family", "Personal-Social")
			.put("Lasting-Personal", "Personal-Social")

			.put("Employment", "ORG-Affiliation")
			.put("Ownership", "ORG-Affiliation")
			.put("Founder", "ORG-Affiliation")
			.put("Student-Alum", "ORG-Affiliation")
			.put("Sports-Affiliation", "ORG-Affiliation")
			.put("Investor-Shareholder", "ORG-Affiliation")
			.put("Membership", "ORG-Affiliation")

			.put("User-Owner-Inventor-Manufacturer", "Agent-Artifact")

			.put("Citizen-Resident-Religion-Ethnicity", "Gen-Affiliation") // GEN-AFF, GPE-AFF
			.put("Org-Location-Origin", "Gen-Affiliation") // GEN-AFF, GPE-AFF

			.build();

	public static String getAceTypeFromSubType(String strAceSubType) {
		if(aceSubtype2type.containsKey(strAceSubType))
			return aceSubtype2type.get(strAceSubType);
		else
			return "N/A";
	}

}
