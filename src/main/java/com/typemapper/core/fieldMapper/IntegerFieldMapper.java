package com.typemapper.core.fieldMapper;

import org.apache.log4j.Logger;

public class IntegerFieldMapper implements FieldMapper {

    private static final Logger LOG = Logger.getLogger(IntegerFieldMapper.class);

    @Override
    public Object mapField(final String string, final Class clazz) {
        if (string == null) {
            return null;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            LOG.error("Could not convert " + string + " to int.", e);
        }

        return null;
    }

}
