
SELECT
    AT_X_ExceptionRecord.X_riskClass_I          AS risk_class,
    AT_X_ExceptionRecord.X_description_S        AS description,
    AT_X_ExceptionRecord.X_exceptionCategory_I  AS exceptionCategory,
    AT_X_ExceptionRecord.creation_time          AS creation_time,
    AT_X_ExceptionRecord.X_exceptionResult_I    AS result,
    AT_X_ExceptionRecord.X_excClassification_I  AS classification,
    AT_X_ExceptionRecord.X_status_I             AS status,
    AT_X_ExceptionRecord.X_capaId_S             AS capaId,
    AT_X_ExceptionRecord.atr_key                AS atr_key

FROM AT_X_ExceptionRecord
 LEFT JOIN AT_X_ESignatureContext        ON AT_X_ExceptionRecord.atr_key = AT_X_ESignatureContext.X_ckey_I AND AT_X_ESignatureContext.X_cname_S = 'ATRow#X_ExceptionRecord'
 INNER JOIN AT_X_ExceptionObjectRelation ON AT_X_ExceptionRecord.atr_key = AT_X_ExceptionObjectRelation.X_exceptionRecord_64
 INNER JOIN AT_X_RtUnitProcedure         ON AT_X_RtUnitProcedure.atr_key = AT_X_ExceptionObjectRelation.X_ctxKey_I
 INNER JOIN AT_X_RtProcedure             ON AT_X_RtProcedure.atr_key = AT_X_RtUnitProcedure.X_parent_64

 WHERE AT_X_ExceptionObjectRelation.X_ctxName_S =    'ATRow#X_RtUnitProcedure'
  AND AT_X_ExceptionObjectRelation.X_status_I       IN :statusCodes
  AND AT_X_ExceptionObjectRelation.X_riskClass_I    IN :riskCodes
  AND AT_X_RtProcedure.X_controlRecipe_113          IN :controlRecipes
  AND (:creationTimeFrom IS NULL OR AT_X_ExceptionRecord.creation_time >= :creationTimeFrom)
  AND (:creationTimeTo   IS NULL OR AT_X_ExceptionRecord.creation_time <= :creationTimeTo)

UNION ALL
SELECT
    AT_X_ExceptionRecord.X_riskClass_I          AS risk_class,
    AT_X_ExceptionRecord.X_description_S        AS description,
    AT_X_ExceptionRecord.X_exceptionCategory_I  AS exceptionCategory,
    AT_X_ExceptionRecord.creation_time          AS creation_time,
    AT_X_ExceptionRecord.X_exceptionResult_I    AS result,
    AT_X_ExceptionRecord.X_excClassification_I  AS classification,
    AT_X_ExceptionRecord.X_status_I             AS status,
    AT_X_ExceptionRecord.X_capaId_S             AS capaId,
    AT_X_ExceptionRecord.atr_key                AS atr_key

FROM AT_X_ExceptionRecord
 LEFT JOIN AT_X_ESignatureContext        ON AT_X_ExceptionRecord.atr_key = AT_X_ESignatureContext.X_ckey_I AND AT_X_ESignatureContext.X_cname_S = 'ATRow#X_ExceptionRecord'
 INNER JOIN AT_X_ExceptionObjectRelation ON AT_X_ExceptionRecord.atr_key = AT_X_ExceptionObjectRelation.X_exceptionRecord_64
 INNER JOIN AT_X_RtOperation             ON AT_X_RtOperation.atr_key = AT_X_ExceptionObjectRelation.X_ctxKey_I
 INNER JOIN AT_X_RtUnitProcedure         ON AT_X_RtUnitProcedure.atr_key = AT_X_RtOperation.X_parent_64
 INNER JOIN AT_X_RtProcedure             ON AT_X_RtProcedure.atr_key = AT_X_RtUnitProcedure.X_parent_64

 WHERE AT_X_ExceptionObjectRelation.X_ctxName_S = 'ATRow#X_RtOperation'
  AND AT_X_ExceptionObjectRelation.X_status_I    IN :statusCodes
  AND AT_X_ExceptionObjectRelation.X_riskClass_I IN :riskCodes
  AND AT_X_RtProcedure.X_controlRecipe_113       IN :controlRecipes
  AND (:creationTimeFrom IS NULL OR AT_X_ExceptionRecord.creation_time >= :creationTimeFrom)
  AND (:creationTimeTo   IS NULL OR AT_X_ExceptionRecord.creation_time <= :creationTimeTo)

UNION ALL

SELECT
    AT_X_ExceptionRecord.X_riskClass_I          AS risk_class,
    AT_X_ExceptionRecord.X_description_S        AS description,
    AT_X_ExceptionRecord.X_exceptionCategory_I  AS exceptionCategory,
    AT_X_ExceptionRecord.creation_time          AS creation_time,
    AT_X_ExceptionRecord.X_exceptionResult_I    AS result,
    AT_X_ExceptionRecord.X_excClassification_I  AS classification,
    AT_X_ExceptionRecord.X_status_I             AS status,
    AT_X_ExceptionRecord.X_capaId_S             AS capaId,
    AT_X_ExceptionRecord.atr_key                AS atr_key

FROM AT_X_ExceptionRecord
 LEFT JOIN AT_X_ESignatureContext        ON AT_X_ExceptionRecord.atr_key = AT_X_ESignatureContext.X_ckey_I AND AT_X_ESignatureContext.X_cname_S = 'ATRow#X_ExceptionRecord'
 INNER JOIN AT_X_ExceptionObjectRelation ON AT_X_ExceptionRecord.atr_key = AT_X_ExceptionObjectRelation.X_exceptionRecord_64
 INNER JOIN AT_X_RtPhase                 ON AT_X_RtPhase.atr_key = AT_X_ExceptionObjectRelation.X_ctxKey_I
 INNER JOIN AT_X_RtOperation             ON AT_X_RtOperation.atr_key = AT_X_RtPhase.X_parent_64
 INNER JOIN AT_X_RtUnitProcedure         ON AT_X_RtUnitProcedure.atr_key = AT_X_RtOperation.X_parent_64
 INNER JOIN AT_X_RtProcedure             ON AT_X_RtProcedure.atr_key = AT_X_RtUnitProcedure.X_parent_64

 WHERE AT_X_ExceptionObjectRelation.X_ctxName_S = 'ATRow#X_RtPhase'
  AND AT_X_ExceptionObjectRelation.X_status_I    IN :statusCodes
  AND AT_X_ExceptionObjectRelation.X_riskClass_I IN :riskCodes
  AND AT_X_RtProcedure.X_controlRecipe_113       IN :controlRecipes
  AND (:creationTimeFrom IS NULL OR AT_X_ExceptionRecord.creation_time >= :creationTimeFrom)
  AND (:creationTimeTo   IS NULL OR AT_X_ExceptionRecord.creation_time <= :creationTimeTo)
