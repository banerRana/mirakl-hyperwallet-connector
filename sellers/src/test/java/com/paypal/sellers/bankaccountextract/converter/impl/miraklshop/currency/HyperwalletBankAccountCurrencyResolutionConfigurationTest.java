package com.paypal.sellers.bankaccountextract.converter.impl.miraklshop.currency;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HyperwalletBankAccountCurrencyResolutionConfigurationTest {

	private HyperwalletBankAccountCurrencyResolutionConfiguration testObj;

	@Test
	void constructor_shouldSetGlobalCurrencyPriority_whenNoPerCountryEntryExists() {
		// Given
		testObj = new HyperwalletBankAccountCurrencyResolutionConfiguration("USD, EUR");

		// When
		List<String> result = testObj.getGlobalCurrencyPriority();
		Map<String, List<String>> perCountryResult = testObj.getPerCountryCurrencyPriority();

		// Then
		assertThat(result).containsExactly("USD", "EUR");
		assertThat(perCountryResult).isEmpty();
	}

	@Test
	void constructor_shouldSetGlobalCurrencyPriority_whenOnlyOneCurrencyIsDefined() {
		// Given
		testObj = new HyperwalletBankAccountCurrencyResolutionConfiguration("USD");

		// When
		List<String> result = testObj.getGlobalCurrencyPriority();

		// Then
		assertThat(result).containsExactly("USD");
	}

	@Test
	void constructor_shouldSetPerCountryCurrencyPriority_whenPerCountryEntryExists() {
		// Given
		testObj = new HyperwalletBankAccountCurrencyResolutionConfiguration("US:USD;CA:CAD,USD");

		// When
		Map<String, List<String>> perCountryResult = testObj.getPerCountryCurrencyPriority();
		List<String> globalResult = testObj.getGlobalCurrencyPriority();

		// Then
		assertThat(perCountryResult.get("US")).containsExactly("USD");
		assertThat(perCountryResult.get("CA")).containsExactly("CAD", "USD");
		assertThat(globalResult).isEmpty();
	}

	@Test
	void constructor_shouldSupportMultipleGlobalAndPerCountryConfigurations() {
		// Given
		testObj = new HyperwalletBankAccountCurrencyResolutionConfiguration("GB,CAD;US:USD;CA:CAD,USD");

		// When
		Map<String, List<String>> perCountryResult = testObj.getPerCountryCurrencyPriority();
		List<String> globalResult = testObj.getGlobalCurrencyPriority();

		// Then
		assertThat(perCountryResult.get("US")).containsExactly("USD");
		assertThat(perCountryResult.get("CA")).containsExactly("CAD", "USD");
		assertThat(globalResult).containsExactly("GB", "CAD");
	}

}
