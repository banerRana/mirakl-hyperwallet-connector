package com.paypal.sellers.bankaccountextract.service.strategies;

import com.hyperwallet.clientsdk.Hyperwallet;
import com.hyperwallet.clientsdk.model.HyperwalletBankAccount;
import com.paypal.infrastructure.hyperwallet.api.HyperwalletSDKUserService;
import com.paypal.infrastructure.mail.MailNotificationUtil;
import com.paypal.infrastructure.strategy.StrategyExecutor;
import com.paypal.sellers.sellersextract.model.SellerModel;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Strategy class that updates an existing bank account into hyperwallet
 */
@Service
public class HyperWalletUpdateBankAccountServiceStrategyBankAccount
		extends AbstractHyperwalletBankAccountRetryApiStrategy {

	protected HyperWalletUpdateBankAccountServiceStrategyBankAccount(
			final StrategyExecutor<SellerModel, HyperwalletBankAccount> sellerModelToHyperwalletBankAccountStrategyExecutor,
			final HyperwalletSDKUserService hyperwalletSDKUserService,
			final MailNotificationUtil mailNotificationUtil) {
		super(sellerModelToHyperwalletBankAccountStrategyExecutor, hyperwalletSDKUserService, mailNotificationUtil);
	}

	/**
	 * Checks whether the strategy must be executed based on the {@code source}
	 * @param seller the source object
	 * @return returns whether the strategy is applicable or not
	 */
	@Override
	public boolean isApplicable(final SellerModel seller) {
		return Objects.nonNull(seller.getBankAccountDetails().getToken());
	}

	@Override
	protected HyperwalletBankAccount callHyperwalletAPI(final String hyperwalletProgram,
			final HyperwalletBankAccount hyperwalletBankAccount) {
		final Hyperwallet hyperwallet = hyperwalletSDKUserService
				.getHyperwalletInstanceByHyperwalletProgram(hyperwalletProgram);
		return hyperwallet.updateBankAccount(hyperwalletBankAccount);
	}

}
