package com.paypal.kyc.service.documents.files.hyperwallet.impl;

import com.google.gson.Gson;
import com.hyperwallet.clientsdk.Hyperwallet;
import com.hyperwallet.clientsdk.HyperwalletException;
import com.hyperwallet.clientsdk.model.HyperwalletVerificationDocument;
import com.paypal.infrastructure.hyperwallet.api.HyperwalletSDKUserService;
import com.paypal.infrastructure.mail.MailNotificationUtil;
import com.paypal.kyc.model.KYCDocumentBusinessStakeHolderInfoModel;
import com.paypal.kyc.model.KYCDocumentInfoModel;
import com.paypal.kyc.model.KYCDocumentSellerInfoModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Class that mocks behaviour of sending KYC documents to mockserver instead of sending
 * them to HW
 */
@Profile({ "qa" })
@Slf4j
@Service("hyperwalletDocumentUploadServiceMock")
public class HyperwalletDocumentUploadServiceMockImpl extends HyperwalletDocumentUploadServiceImpl {

	private static final String FAILING_FILES = "fail";

	private static final String HYPERWALLET_PUSH_DOCUMENTS_BSTK = "/hyperwallet/v4/{userToken}/{bstToken}/documents";

	private static final String HYPERWALLET_PUSH_DOCUMENTS_SELLER = "/hyperwallet/v4/{userToken}/documents";

	private final String mockServerUrl;

	private final RestTemplate restTemplate;

	public HyperwalletDocumentUploadServiceMockImpl(final HyperwalletSDKUserService hyperwalletSDKUserService,
			final MailNotificationUtil kycMailNotificationUtil, @Value("${mockserver.url}") final String mockServerUrl,
			final RestTemplate restTemplate) {
		super(hyperwalletSDKUserService, kycMailNotificationUtil);
		this.mockServerUrl = mockServerUrl;
		this.restTemplate = restTemplate;
	}

	@Override
	protected void invokeHyperwalletAPI(final KYCDocumentInfoModel kycDocumentInfoModel,
			final List<HyperwalletVerificationDocument> hyperwalletVerificationDocuments,
			final Hyperwallet hyperwallet) {
		final String postURL = getPostURL(kycDocumentInfoModel);

		if (checkFailingFiles(hyperwalletVerificationDocuments)) {
			throw new HyperwalletException("Something bad happened");
		}
		final Gson gsonConverter = new Gson();
		restTemplate.postForObject(getMockServerUrl() + postURL, gsonConverter.toJson(hyperwalletVerificationDocuments),
				Object.class);
	}

	private String getPostURL(final KYCDocumentInfoModel kycDocumentInfoModel) {
		if (kycDocumentInfoModel instanceof KYCDocumentBusinessStakeHolderInfoModel) {
			final KYCDocumentBusinessStakeHolderInfoModel kycDocumentBusinessStakeHolderInfoModel = (KYCDocumentBusinessStakeHolderInfoModel) kycDocumentInfoModel;
			return HYPERWALLET_PUSH_DOCUMENTS_BSTK
					.replace("{userToken}", kycDocumentBusinessStakeHolderInfoModel.getUserToken())
					.replace("{bstToken}", kycDocumentBusinessStakeHolderInfoModel.getToken());
		}
		else {
			final KYCDocumentSellerInfoModel kycDocumentSellerInfoModel = (KYCDocumentSellerInfoModel) kycDocumentInfoModel;
			return HYPERWALLET_PUSH_DOCUMENTS_SELLER.replace("{userToken}", kycDocumentSellerInfoModel.getUserToken());
		}
	}

	private boolean checkFailingFiles(final List<HyperwalletVerificationDocument> originalFiles) {
		//@formatter:off
		return Optional.ofNullable(originalFiles).orElse(List.of()).stream()
				.map(HyperwalletVerificationDocument::getUploadFiles)
				.filter(Objects::nonNull)
				.map(Map::values)
				.flatMap(Collection::stream)
				.anyMatch(fileName -> fileName.contains(FAILING_FILES));
		//@formatter:on
	}

	protected String getMockServerUrl() {
		return mockServerUrl;
	}

}
