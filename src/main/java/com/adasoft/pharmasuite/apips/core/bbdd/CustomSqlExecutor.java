package com.adasoft.pharmasuite.apips.core.bbdd;

import com.rockwell.mes.commons.base.ifc.sql.ColumnDescriptor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CustomSqlExecutor extends SqlExecutor{
    public List<String[]> fetchFormatted(String sql, ColumnDescriptor[] cols) {
        return executeFormatted(sql, cols);
    }

    public List<Map<String,String>> fetchAsMap(String sql, ColumnDescriptor[] cols) {
        List<String[]> rows = fetchFormatted(sql, cols);
        List<Map<String,String>> out = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ColumnDescriptor cd : cols) names.add(cd.getName());

        for (String[] row : rows) {
            Map<String,String> m = new LinkedHashMap<>();
            for (int i = 0; i < names.size(); i++) {
                m.put(names.get(i), row[i]);
            }
            out.add(m);
        }
        return out;
    }


    public long getCount(String sql) {
        return executeSqlCount(sql);
    }
}
