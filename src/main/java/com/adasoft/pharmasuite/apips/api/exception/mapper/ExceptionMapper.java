package com.adasoft.pharmasuite.apips.api.exception.mapper;

import com.adasoft.pharmasuite.apips.api.common.domain.ErrorApi;
import com.adasoft.pharmasuite.apips.api.common.mapper.BaseMapper;
import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionComment;
import com.adasoft.pharmasuite.apips.api.exception.domain.ExceptionPS;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import com.rockwell.mes.commons.base.ifc.choicelist.IMESChoiceElement;
import com.rockwell.mes.commons.deviation.ifc.exceptionrecording.IMESExceptionComment;
import com.rockwell.mes.commons.deviation.ifc.exceptionrecording.IMESExceptionRecord;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class ExceptionMapper extends BaseMapper {

    public ExceptionPS toDto(IMESExceptionRecord exceptionPS) {
        List<ErrorApi> errors = new ArrayList<>();
        ExceptionPS exp = new ExceptionPS();
        try {
            exp = ExceptionPS.builder()
                    .key(Optional.of(exceptionPS.getKey()).orElse(0L))
                    .creationDate(getDateTime(exceptionPS.getCreationTime()))
                    .description(exceptionPS.getDescription())
                    .risk(exceptionPS.getRiskClassAsMeaning())
                    .category(Optional.ofNullable(exceptionPS.getExceptionCategory())
                            .map(IMESChoiceElement::getMeaning).orElse(""))
                    .result(Optional.ofNullable(exceptionPS.getExceptionResult())
                            .map(IMESChoiceElement::getMeaning).orElse(""))
                    .classification(Optional.ofNullable(exceptionPS.getExcClassificationAsMeaning()).orElse(""))
                    .status(getState(exceptionPS, errors))
                    .capaId(Optional.ofNullable(exceptionPS.getCapaId()).orElse(""))
                    .reference("") // TODO: ALEX
                    //.comments(getExceptionCommentList(exceptionPS.getExceptionComments(), errors))
                    .build();
        } catch (Exception e) {
            LogManagement.error(format(ERROR_MAPPER, e.getMessage()), this);
            errors.add(ErrorApi.builder().message(e.getMessage()).build());
        }
        exp.setErrors(errors);
        exp.setTimeServer(OffsetDateTime.now(ZoneOffset.UTC));
        return exp;
    }

    private String getState(IMESExceptionRecord item,  List<ErrorApi> errors) {
        try {
            if (item.getStatusAsMeaning() != null && !item.getStatusAsMeaning().isEmpty()) {
                return item.getStatusAsMeaning();
            }
        } catch (Exception e) {
            String error = format(ERROR_MAPPER, e.getMessage());
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), this.getClass());
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }
        return "";
    }


    private List<ExceptionComment> getExceptionCommentList(List<IMESExceptionComment> listExceptionCommentPS,  List<ErrorApi> errors) {
        List<ExceptionComment> listResult = new ArrayList<>();
        try {
            if (listExceptionCommentPS != null) {
                for (IMESExceptionComment item : listExceptionCommentPS) {
                    listResult.add(ExceptionComment.builder()
                                    .description(Optional.ofNullable(item.getComment()).orElse(""))
                            .externalIdentifier(Optional.ofNullable(item.getExternalIdentifier()).orElse(""))
                            .creationTime(getDateTime(item.getCreationTime()))
                            .build());
                }
            }
        } catch (Exception e) {
            String error = format(ERROR_MAPPER, e.getMessage());
            LogManagement.error(format(ERROR_MAPPER_VALUE, error, e.getMessage()), this.getClass());
            errors.add(ErrorApi.builder().description(error).message(e.getMessage()).build());
        }
        return listResult;
    }

}
