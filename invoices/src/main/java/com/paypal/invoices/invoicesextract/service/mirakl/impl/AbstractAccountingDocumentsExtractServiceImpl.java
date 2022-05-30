package com.paypal.invoices.invoicesextract.service.mirakl.impl;

import com.mirakl.client.core.exception.MiraklApiException;
import com.mirakl.client.mmp.domain.accounting.document.MiraklAccountingDocumentPaymentStatus;
import com.mirakl.client.mmp.domain.accounting.document.MiraklAccountingDocumentType;
import com.mirakl.client.mmp.domain.shop.MiraklShop;
import com.mirakl.client.mmp.domain.shop.MiraklShops;
import com.mirakl.client.mmp.operator.request.payment.invoice.MiraklGetInvoicesRequest;
import com.mirakl.client.mmp.request.payment.invoice.MiraklAccountingDocumentState;
import com.mirakl.client.mmp.request.shop.MiraklGetShopsRequest;
import com.paypal.infrastructure.converter.Converter;
import com.paypal.infrastructure.mail.MailNotificationUtil;
import com.paypal.infrastructure.sdk.mirakl.MiraklMarketplacePlatformOperatorApiWrapper;
import com.paypal.infrastructure.sdk.mirakl.domain.invoice.HMCMiraklInvoice;
import com.paypal.infrastructure.sdk.mirakl.domain.invoice.HMCMiraklInvoices;
import com.paypal.infrastructure.util.MiraklLoggingErrorsUtil;
import com.paypal.invoices.invoicesextract.model.AccountingDocumentModel;
import com.paypal.invoices.invoicesextract.model.InvoiceTypeEnum;
import com.paypal.invoices.invoicesextract.service.mirakl.MiraklAccountingDocumentExtractService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.paypal.infrastructure.constants.HyperWalletConstants.MIRAKL_MAX_RESULTS_PER_PAGE;

@Slf4j
@Service
public abstract class AbstractAccountingDocumentsExtractServiceImpl<T extends AccountingDocumentModel>
		implements MiraklAccountingDocumentExtractService<T> {

	protected final Converter<MiraklShop, AccountingDocumentModel> miraklShopToAccountingModelConverter;

	protected final MiraklMarketplacePlatformOperatorApiWrapper miraklMarketplacePlatformOperatorApiClient;

	protected final MailNotificationUtil invoicesMailNotificationUtil;

	@Value("${invoices.searchinvoices.maxdays}")
	protected int maxNumberOfDaysForInvoiceIdSearch;

	protected AbstractAccountingDocumentsExtractServiceImpl(
			final Converter<MiraklShop, AccountingDocumentModel> miraklShopToAccountingModelConverter,
			final MiraklMarketplacePlatformOperatorApiWrapper miraklMarketplacePlatformOperatorApiClient,
			final MailNotificationUtil invoicesMailNotificationUtil) {
		this.miraklShopToAccountingModelConverter = miraklShopToAccountingModelConverter;
		this.miraklMarketplacePlatformOperatorApiClient = miraklMarketplacePlatformOperatorApiClient;
		this.invoicesMailNotificationUtil = invoicesMailNotificationUtil;
	}

	@Override
	public List<T> extractAccountingDocuments(final Date delta) {
		final List<T> invoices = getAccountingDocuments(delta);
		final Map<String, Pair<String, String>> mapShopDestinationToken = getMapDestinationTokens(invoices);

		return associateBillingDocumentsWithTokens(invoices, mapShopDestinationToken);
	}

	private Map<String, Pair<String, String>> getMapDestinationTokens(final List<T> creditNotes) {
		final List<MiraklShop> shops = getMiraklShops(creditNotes);
		return mapShopsWithDestinationToken(shops);
	}

	protected List<T> filterOnlyMappableDocuments(final List<T> invoices, final Set<String> shopIds) {

		//@formatter:off
		log.warn("Credit notes documents with ids [{}] should be skipped because are lacking hw-program or bank account token", invoices.stream()
				.filter(invoice -> !shopIds.contains(invoice.getShopId()))
				.map(AccountingDocumentModel::getInvoiceNumber)
				.collect(Collectors.joining(",")));
		//@formatter:on

		//@formatter:off
		return invoices.stream()
				.filter(invoice -> shopIds.contains(invoice.getShopId()))
				.collect(Collectors.toList());
		//@formatter:on
	}

	@NonNull
	protected List<MiraklShop> getMiraklShops(final List<T> invoices) {
		//@formatter:off
		final Set<String> shopIds = invoices.stream()
				.map(AccountingDocumentModel::getShopId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		log.info("Retrieving information of shops [{}] for invoices [{}]", String.join(",", shopIds),
				invoices.stream()
						.map(AccountingDocumentModel::getInvoiceNumber)
						.collect(Collectors.joining(",")));
		//@formatter:on

		return getAllShops(shopIds);
	}

	protected List<MiraklShop> getAllShops(final Set<String> shopIds) {
		if (shopIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<List<String>> partitionedShopIds = ListUtils.partition(shopIds.stream().collect(Collectors.toList()), 100);
		//@formatter:off
		return partitionedShopIds.stream()
				.map(this::getAllShopsWithoutPartitioning)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		//@formatter:on
	}

	protected List<MiraklShop> getAllShopsWithoutPartitioning(final List<String> shopIds) {
		if (shopIds.isEmpty()) {
			return Collections.emptyList();
		}

		try {
			final MiraklGetShopsRequest request = createShopRequest(shopIds.stream().collect(Collectors.toSet()));
			final MiraklShops miraklShops = miraklMarketplacePlatformOperatorApiClient.getShops(request);
			return miraklShops.getShops();
		}
		catch (final MiraklApiException ex) {
			List<String> sortedShopIds = shopIds.stream().sorted().collect(Collectors.toList());
			log.error(String.format("Something went wrong getting information of shops [%s]%n%s",
					String.join(",", sortedShopIds), MiraklLoggingErrorsUtil.stringify(ex)), ex);
			invoicesMailNotificationUtil.sendPlainTextEmail("Issue detected getting shops in Mirakl",
					String.format("Something went wrong getting information of shops [%s]%n%s",
							String.join(",", sortedShopIds), MiraklLoggingErrorsUtil.stringify(ex)));

			return Collections.emptyList();
		}
	}

	protected Map<String, Pair<String, String>> mapShopsWithDestinationToken(final List<MiraklShop> shops) {
		//@formatter:off
		return Stream.ofNullable(shops)
				.flatMap(Collection::stream)
				.map(miraklShopToAccountingModelConverter::convert)
				.filter(invoiceModel -> Objects.nonNull(invoiceModel.getDestinationToken()) && Objects.nonNull(invoiceModel.getHyperwalletProgram()))
				.collect(Collectors.toMap(AccountingDocumentModel::getShopId, accountingDocument -> Pair.of(accountingDocument.getDestinationToken(), accountingDocument.getHyperwalletProgram()), (i1, i2) -> i1));
		//@formatter:on
	}

	@NonNull
	protected MiraklGetInvoicesRequest createAccountingDocumentRequest(final Date delta,
			final InvoiceTypeEnum invoiceType) {
		final MiraklGetInvoicesRequest miraklGetInvoicesRequest = new MiraklGetInvoicesRequest();
		miraklGetInvoicesRequest.setStartDate(delta);
		miraklGetInvoicesRequest.setPaymentStatus(MiraklAccountingDocumentPaymentStatus.PENDING);
		miraklGetInvoicesRequest.addState(MiraklAccountingDocumentState.COMPLETE);
		miraklGetInvoicesRequest.setType(EnumUtils.getEnum(MiraklAccountingDocumentType.class, invoiceType.name()));
		miraklGetInvoicesRequest.setMax(MIRAKL_MAX_RESULTS_PER_PAGE);

		return miraklGetInvoicesRequest;
	}

	@NonNull
	protected MiraklGetShopsRequest createShopRequest(final Set<String> shopIds) {
		final MiraklGetShopsRequest request = new MiraklGetShopsRequest();
		request.setShopIds(shopIds);
		request.setPaginate(false);
		return request;
	}

	protected List<HMCMiraklInvoice> getInvoicesForDateAndType(final Date delta, final InvoiceTypeEnum invoiceType) {
		final List<HMCMiraklInvoice> invoices = new ArrayList<>();

		int offset = 0;
		final MiraklGetInvoicesRequest accountingDocumentRequest = createAccountingDocumentRequest(delta, invoiceType);
		while (true) {
			accountingDocumentRequest.setOffset(offset);
			final HMCMiraklInvoices receivedInvoices = miraklMarketplacePlatformOperatorApiClient
					.getInvoices(accountingDocumentRequest);
			invoices.addAll(receivedInvoices.getHmcInvoices());

			if (receivedInvoices.getTotalCount() <= invoices.size()) {
				break;
			}
			offset += MIRAKL_MAX_RESULTS_PER_PAGE;
		}

		return invoices;
	}

	protected List<T> associateBillingDocumentsWithTokens(final List<T> invoices,
			final Map<String, Pair<String, String>> mapShopDestinationToken) {

		final List<T> filteredInvoices = filterOnlyMappableDocuments(invoices, mapShopDestinationToken.keySet());
		//@formatter:off
		return filteredInvoices.stream()
				.map(invoiceModel -> (T) invoiceModel.toBuilder()
						.destinationToken(mapShopDestinationToken.get(invoiceModel.getShopId()).getLeft())
						.hyperwalletProgram(mapShopDestinationToken.get(invoiceModel.getShopId()).getRight())
						.build())
				.collect(Collectors.toList());
		//@formatter:on
	}

	protected List<T> getAccountingDocuments(final Date delta) {
		final List<HMCMiraklInvoice> invoices = getInvoicesForDateAndType(delta, getInvoiceType());

		//@formatter:off
		return invoices.stream()
				.map(getMiraklInvoiceToAccountingModelConverter()::convert)
				.collect(Collectors.toList());
		//@formatter:on
	}

	@Override
	public Collection<T> extractAccountingDocuments(List<String> ids) {
		final List<HMCMiraklInvoice> invoices = getInvoicesForDateAndType(getTimeRangeForFindByIdInvoices(),
				getInvoiceType());

		//@formatter:off
		return invoices.stream()
				.filter(invoice -> ids.contains(invoice.getId()))
				.map(getMiraklInvoiceToAccountingModelConverter()::convert)
				.collect(Collectors.toList());
		//@formatter:on
	}

	private Date getTimeRangeForFindByIdInvoices() {
		return Date.from(LocalDateTime.now().minusMinutes(maxNumberOfDaysForInvoiceIdSearch).toInstant(ZoneOffset.UTC));
	}

	protected abstract InvoiceTypeEnum getInvoiceType();

	protected abstract Converter<HMCMiraklInvoice, T> getMiraklInvoiceToAccountingModelConverter();

}
