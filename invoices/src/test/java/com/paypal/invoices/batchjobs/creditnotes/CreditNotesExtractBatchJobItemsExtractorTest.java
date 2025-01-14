package com.paypal.invoices.batchjobs.creditnotes;

import com.paypal.infrastructure.batchjob.BatchJobContext;
import com.paypal.invoices.invoicesextract.model.CreditNoteModel;
import com.paypal.invoices.invoicesextract.service.mirakl.MiraklAccountingDocumentExtractService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditNotesExtractBatchJobItemsExtractorTest {

	private static final Date DELTA = new Date();

	@InjectMocks
	private CreditNotesExtractBatchJobItemsExtractor testObj;

	@Mock
	private MiraklAccountingDocumentExtractService<CreditNoteModel> miraklAccountingDocumentCreditNotesExtractServiceMock;

	@Mock
	private CreditNoteModel creditNoteModelMock1, creditNoteModelMock2;

	@Mock
	private BatchJobContext batchJobContextMock;

	@Test
	void getItems_ShouldReturnACollectionOfCreditNoteExtractJobItemForTheGivenDelta() {

		when(miraklAccountingDocumentCreditNotesExtractServiceMock.extractAccountingDocuments(DELTA))
				.thenReturn(List.of(creditNoteModelMock1, creditNoteModelMock2));

		final Collection<CreditNoteExtractJobItem> result = testObj.getItems(batchJobContextMock, DELTA);

		assertThat(result.stream().map(CreditNoteExtractJobItem::getItem))
				.containsExactlyInAnyOrder(creditNoteModelMock1, creditNoteModelMock2);
	}

}
