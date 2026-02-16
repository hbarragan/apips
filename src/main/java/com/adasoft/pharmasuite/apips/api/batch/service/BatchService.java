package com.adasoft.pharmasuite.apips.api.batch.service;

import com.adasoft.pharmasuite.apips.api.batch.domain.*;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface BatchService {

    //odatafilter
    ResponseEntity<PageResponseOdata<Batch>> getFilteredBatchOData(final OdataPage odataPage);
    ResponseEntity<PageResponseOdata<TransactionHistory>> getFilteredTransactionHistoryOData(final OdataPage odataPage);
    //details
    ResponseEntity<List<Sublot>> getFilteredSubLot(SublotFilter filter);
    ResponseEntity<List<TransactionHistory>> getFilteredTransactionHistory(TransactionHistoryFilter filter);




    public List<Batch> getAllBatch(BatchFilterDTO filter);

}
