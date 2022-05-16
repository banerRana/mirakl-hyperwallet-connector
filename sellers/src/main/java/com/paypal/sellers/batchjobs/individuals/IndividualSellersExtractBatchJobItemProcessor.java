package com.paypal.sellers.batchjobs.individuals;

import com.paypal.infrastructure.batchjob.BatchJobContext;
import com.paypal.infrastructure.batchjob.BatchJobItemProcessor;
import com.paypal.infrastructure.service.TokenSynchronizationService;
import com.paypal.sellers.sellersextract.model.SellerModel;
import com.paypal.sellers.sellersextract.service.strategies.HyperWalletUserServiceStrategyExecutor;
import org.springframework.stereotype.Component;

/**
 * Individual sellers extract batch job item processor for creating the users in HW
 */
@Component
public class IndividualSellersExtractBatchJobItemProcessor
		implements BatchJobItemProcessor<BatchJobContext, IndividualSellersExtractJobItem> {

	private final HyperWalletUserServiceStrategyExecutor hyperWalletUserServiceStrategyExecutor;

	private final TokenSynchronizationService<SellerModel> sellersTokenSynchronizationService;

	public IndividualSellersExtractBatchJobItemProcessor(
			final HyperWalletUserServiceStrategyExecutor hyperWalletUserServiceStrategyExecutor,
			final TokenSynchronizationService<SellerModel> sellersTokenSynchronizationService) {
		this.hyperWalletUserServiceStrategyExecutor = hyperWalletUserServiceStrategyExecutor;
		this.sellersTokenSynchronizationService = sellersTokenSynchronizationService;
	}

	/**
	 * Processes the {@link IndividualSellersExtractJobItem} with the
	 * {@link HyperWalletUserServiceStrategyExecutor}
	 * @param ctx The {@link BatchJobContext}
	 * @param jobItem The {@link IndividualSellersExtractJobItem}
	 */
	@Override
	public void processItem(final BatchJobContext ctx, final IndividualSellersExtractJobItem jobItem) {
		final SellerModel sellerModel = sellersTokenSynchronizationService.synchronizeToken(jobItem.getItem());
		hyperWalletUserServiceStrategyExecutor.execute(sellerModel);
	}

}
