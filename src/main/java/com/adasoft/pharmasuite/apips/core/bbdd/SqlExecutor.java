package com.adasoft.pharmasuite.apips.core.bbdd;

import com.rockwell.mes.commons.base.ifc.exceptions.MESRuntimeException;
import com.rockwell.mes.commons.base.ifc.services.PCContext;
import com.rockwell.mes.commons.base.ifc.sql.ColumnDescriptor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SqlExecutor {

    public List<String[]> executeRaw(String sql,
                                     ColumnDescriptor[] descriptors)
    {
        List<String[]> raw;
        try {
            @SuppressWarnings("unchecked")
            Vector<String[]> vec = PCContext.getFunctions().getArrayDataFromActive(sql);
            raw = adjustTimeZonesForDateTimeColumns(descriptors, vec);
        } catch (RuntimeException ex) {
            String msg = "SQL-query failed:\n" + sql;
            throw new MESRuntimeException(msg, ex);
        }
        return raw;
    }

    public long executeSqlCount(String sql){
        try {
            @SuppressWarnings("unchecked")
            Vector<String[]> vec = PCContext.getFunctions().getArrayDataFromActive(sql);
            return vec.size();
        } catch (RuntimeException ex) {
            String msg = "SQL-query failed:\n" + sql;
            throw new MESRuntimeException(msg, ex);
        }
    }


    public List<String[]> executeFormatted(String sql,ColumnDescriptor[] descriptors){
        List<String[]> raw = executeRaw(sql, descriptors);
        return formatRows(descriptors, raw);
    }


    private List<String[]> adjustTimeZonesForDateTimeColumns(ColumnDescriptor[] cds, List<String[]> rows){
        int count = getNumberOfDateTimeColumns(cds);
        if (count == 0) return rows;

        List<String[]> out = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            String[] newRow = new String[row.length - count];
            int inIdx = 0, outIdx = 0;
            while (inIdx < row.length) {
                String val = row[inIdx];
                boolean isDateTime = cds[outIdx].getDataType() == 6;
                if (isDateTime && StringUtils.isNotEmpty(val)) {
                    String colName = cds[outIdx].getName();
                    if (colName.endsWith("_u")) {
                        val = com.rockwell.mes.commons.base.ifc.sql.TimeUtility.replaceTimeZoneInTimeString(val, "UTC");
                    } else {
                        String[] valArray=val.split(";");
                        String zone = valArray[1];
                        val = com.rockwell.mes.commons.base.ifc.sql.TimeUtility.replaceTimeZoneInTimeString(val, zone);
                    }
                }
                newRow[outIdx++] = val;
                inIdx++;
                if (isDateTime) inIdx++;
            }
            out.add(newRow);
        }
        return out;
    }

    private int getNumberOfDateTimeColumns(ColumnDescriptor[] cds) {
        int c = 0;
        for (ColumnDescriptor cd : cds) {
            if (cd.getDataType() == 6) c++;
        }
        return c;
    }

    protected List<String[]> formatRows(ColumnDescriptor[] cds,List<String[]> rows){
        List<String[]> out = new ArrayList<>(rows.size());
        for (String[] row : rows) {
            for (int i = 0; i < cds.length; i++) {
                String cell = row[i];
                if (StringUtils.isNotEmpty(cell)) {
                    row[i] = cds[i].formatColumnValue(cell);
                }
            }
            out.add(row);
        }
        return out;
    }

}
