package com.adasoft.pharmasuite.apips.api.common.service.odata;

import com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry;
import com.adasoft.pharmasuite.apips.core.utils.LogManagement;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;

import java.util.List;
import java.util.Locale;

import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.CONTAINS;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.ENDSWITH;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.EQ;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.GT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.LT;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.NE;
import static com.adasoft.pharmasuite.apips.api.common.domain.odata.FilterRegistry.Op.STARTSWITH;


public class GenericFilterVisitor<F> implements ExpressionVisitor<Object> {

    private final FilterRegistry<F> registry;
    private final F target;

    public GenericFilterVisitor(FilterRegistry<F> registry, F target) {
        this.registry = registry;
        this.target = target;
    }

    // -------- helpers --------
    private static String prop(Member m) {
        List<UriResource> parts = m.getResourcePath().getUriResourceParts();
        if (parts == null || parts.isEmpty()) return null;
        UriResource last = parts.get(parts.size() - 1);
        if (last instanceof UriResourceProperty p) return p.getProperty().getName();
        return last.toString();
    }

    private static String unquote(String raw) {
        if (raw == null) return null;
        return raw.length() >= 2 && raw.startsWith("'") && raw.endsWith("'")
                ? raw.substring(1, raw.length()-1) : raw;
    }

    private void apply(String property, FilterRegistry.Op op, String rawValue) throws Exception {
        var binderOpt = registry.find(property);

        if (binderOpt.isEmpty()) {
            LogManagement.error("Propiedad no soportada en $filter (se ignora): "+ property,this);
            return;
        }

        var binder = binderOpt.get();
        // La validación de operador permitido la hace el wrapper de Builder.bind(...)
        binder.apply(target, op, rawValue);
    }

    private static ODataApplicationException bad(String msg) {
        return new ODataApplicationException(msg, 400, Locale.ROOT);
    }

    private Boolean logErrorAndContinueBool(String msg) {
        LogManagement.error(msg,this);
        return Boolean.TRUE; // valor neutro para AND/OR
    }

    // -------- ExpressionVisitor --------

    @Override
    public Boolean visitBinaryOperator(BinaryOperatorKind op, Object left, Object right)
            throws ExpressionVisitException, ODataApplicationException {

        // 1) AND / OR: los hijos ya han sido visitados y han aplicado sus filtros
        if (op == BinaryOperatorKind.AND || op == BinaryOperatorKind.OR) {
            // left y right serán típicamente Boolean.TRUE; no los necesitamos
            return Boolean.TRUE;
        }

        // 2) Para el resto de operadores sí esperamos Member op Literal
        if (!(left instanceof String prop) || !(right instanceof String lit)) {
            throw bad("Expresión $filter no soportada (se esperaba Member op Literal)");
        }
        String value = unquote(lit);

        try {
            switch (op) {
                case EQ -> apply(prop, EQ, value);
                case NE -> apply(prop, NE, value);
                case GT -> apply(prop, GT, value);
                case GE -> apply(prop, GE, value);
                case LT -> apply(prop, LT, value);
                case LE -> apply(prop, LE, value);
                default -> throw bad("Operador binario no soportado: " + op);
            }
        } catch (Exception e) {
            throw bad("Error in case: " + op + " " + e.getMessage());
        }
        return Boolean.TRUE;
    }

    @Override public Boolean visitUnaryOperator(UnaryOperatorKind op, Object operand)
            throws ExpressionVisitException, ODataApplicationException {
        throw bad("Operador unario no soportado: " + op);
    }

    @Override
    public Boolean visitMethodCall(MethodKind method, List<Object> params)
            throws ExpressionVisitException, ODataApplicationException {

        if (params.size() != 2 || !(params.get(0) instanceof String p) || !(params.get(1) instanceof String lit)) {
            throw bad("Método " + method + " espera (Member, Literal)");
        }
        String value = unquote((String) params.get(1));

        try {
            switch (method) {
                case CONTAINS    -> apply(p, CONTAINS, value);
                case STARTSWITH  -> apply(p, STARTSWITH, value);
                case ENDSWITH    -> apply(p, ENDSWITH, value);
                default -> throw bad("Método no soportado en $filter: " + method);
            }
        } catch (Exception e) {
            throw bad("Error in case: " + method +" " +  e.getMessage());
        }
        return Boolean.TRUE;
    }

    @Override public Object visitMember(Member member) { return prop(member); }
    @Override public Object visitLiteral(Literal literal) { return literal.getText(); }
    @Override public Object visitAlias(String aliasName) { return null; }
    @Override public Object visitTypeLiteral(EdmType type) { return null; }
    @Override public Object visitLambdaExpression(String fn, String var, Expression expr) throws ODataApplicationException {
        throw bad("Lambda (any/all) no soportadas");
    }
    @Override public Object visitLambdaReference(String variableName) { return null; }
    @Override public Object visitEnum(org.apache.olingo.commons.api.edm.EdmEnumType t, List<String> v) { return null; }
    @Override public Object visitBinaryOperator(BinaryOperatorKind op, Object o, List<Object> list) { return null; }
}
