package com.adasoft.pharmasuite.apips.api.order.service;

import com.adasoft.pharmasuite.apips.api.common.domain.PageResponse;
import com.adasoft.pharmasuite.apips.api.common.domain.PageResponseOdata;
import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import com.adasoft.pharmasuite.apips.api.order.domain.*;

import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProcessOrdersService {
    ResponseEntity<PageResponse<ProcessOrder>> getAllProcessOrdersPaged(OrderFilterPaged filter);
    ResponseEntity<List<ProcessOrder>> getAllProcessOrders(OrderFilter filter);
    ResponseEntity<ProcessOrderDetail> getProcessOrderDetail(OrderDetailFilter filter);
    ResponseEntity<PageResponseOdata<ProcessOrder>> getFilteredOData(OdataPage odataPage);

   List<ProcessOrder> getAllProcessOrders(OrderFilterDTO filter);

}
