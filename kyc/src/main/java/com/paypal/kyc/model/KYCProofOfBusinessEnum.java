package com.paypal.kyc.model;

import java.util.List;

/**
 * Enum that defines proof of business types
 */

public enum KYCProofOfBusinessEnum {

	INCORPORATION, BUSINESS_REGISTRATION, OPERATING_AGREEMENT;

	public static List<String> getMiraklFields() {
		return List.of(KYCConstants.HwDocuments.PROOF_OF_BUSINESS);
	}

}
