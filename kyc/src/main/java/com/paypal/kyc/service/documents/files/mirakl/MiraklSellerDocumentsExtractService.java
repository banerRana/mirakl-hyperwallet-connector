package com.paypal.kyc.service.documents.files.mirakl;

import com.paypal.kyc.model.KYCDocumentInfoModel;
import com.paypal.kyc.model.KYCDocumentSellerInfoModel;

import java.util.Date;
import java.util.List;

/**
 * Interface that manages logic of getting seller information related to KYC in Mirakl
 */
public interface MiraklSellerDocumentsExtractService extends MiraklDocumentsExtractService {

	/**
	 * Obtains valid and requiresKYC flagged proof of identity/business sellers since a
	 * delta time
	 * @param delta
	 * @return {@link List< KYCDocumentSellerInfoModel >} valids to be sent to other
	 * system for KYC verification
	 */
	List<KYCDocumentSellerInfoModel> extractProofOfIdentityAndBusinessSellerDocuments(Date delta);

	/**
	 * Retrieves a KYCDocumentInfoModel filled with all the information from a MiraklShop
	 * id
	 * @param shopId Mirakl shop id
	 * @return {@link KYCDocumentInfoModel} Model containing Seller information
	 */
	KYCDocumentInfoModel extractKYCSellerDocuments(final String shopId);

	/**
	 * Sets KYC required verification proof of identity/business flag for seller to false
	 * in Mirakl
	 * @param successFullPushedListOfDocument {@link KYCDocumentSellerInfoModel} seller to
	 * be unchecked from KYC verification
	 */
	void setFlagToPushProofOfIdentityAndBusinessSellerDocumentsToFalse(
			KYCDocumentSellerInfoModel successFullPushedListOfDocument);

}
