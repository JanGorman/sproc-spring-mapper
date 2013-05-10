package com.typemapper.parser.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.List;

import org.apache.log4j.Logger;

import com.typemapper.parser.exception.RowParserException;

public class RowMapper {

    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(RowMapper.class);

    /**
     * @param   rs
     * @param   columnName
     *
     * @return
     *
     * @throws  SQLException
     */
    public final Element mapRow(final ResultSet rs, final String columnName) throws SQLException {
        final Element element = new Element();
        List<String> l;
        try {
            l = ParseUtils.postgresROW2StringList(rs.getString(columnName));
        } catch (RowParserException e) {
            throw new SQLException(e);
        }

        element.setRowList(l);
        return element;
    }

}
