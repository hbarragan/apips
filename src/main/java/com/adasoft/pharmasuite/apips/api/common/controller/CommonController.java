package com.adasoft.pharmasuite.apips.api.common.controller;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.OdataPage;
import jakarta.servlet.http.HttpServletRequest;


public class CommonController {


    public static OdataPage getOdataPage(String filter, String orderBy, Integer top, Integer skip, Boolean count, HttpServletRequest request) {
        OdataPage odata = new OdataPage();
        odata.setOdataFilter(filter);
        odata.setOrderBy(orderBy);
        odata.setTop(top);
        odata.setSkip(skip);
        odata.setCount(count);
        odata.setRequest(request);
        return odata;
    }


}
