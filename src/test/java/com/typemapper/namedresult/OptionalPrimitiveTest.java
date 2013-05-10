package com.typemapper.namedresult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

import com.typemapper.AbstractTest;

import com.typemapper.core.TypeMapper;
import com.typemapper.core.TypeMapperFactory;

import com.typemapper.namedresult.results.ClassWithOptionalPrimitives;

public class OptionalPrimitiveTest extends AbstractTest {

    @Test
    public void testPrimitiveMappings() throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT 1 as i, 2 as l, 'c' as c");
        ResultSet rs = ps.executeQuery();
        TypeMapper<?> mapper = TypeMapperFactory.createTypeMapper(ClassWithOptionalPrimitives.class);
        int i = 0;
        while (rs.next()) {
            ClassWithOptionalPrimitives result = (ClassWithOptionalPrimitives) mapper.mapRow(rs, i++);
            assertNotNull(result);
            assertEquals(Integer.valueOf(1), result.getI().get());
            assertEquals(Long.valueOf(2), result.getL().get());
            assertEquals(Character.valueOf('c'), result.getC().get());
        }
    }

}
