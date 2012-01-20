package com.typemapper.postgres;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import java.sql.SQLException;
import java.sql.Types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.postgresql.util.PGobject;

import com.typemapper.annotations.DatabaseField;
import com.typemapper.annotations.DatabaseType;

public class PgTypeHelper {

    private static final Map<String, Integer> pgGenericTypeNameToSQLTypeMap;

    static {
        Map<String, Integer> m = new HashMap<String, Integer>();
        m.put("int2", Types.SMALLINT);
        m.put("int4", Types.INTEGER);
        m.put("oid", Types.BIGINT);
        m.put("int8", Types.BIGINT);
        m.put("money", Types.DOUBLE);
        m.put("numeric", Types.NUMERIC);
        m.put("float4", Types.REAL);
        m.put("float8", Types.DOUBLE);
        m.put("bpchar", Types.CHAR);
        m.put("varchar", Types.VARCHAR);
        m.put("text", Types.VARCHAR);
        m.put("name", Types.VARCHAR);
        m.put("bytea", Types.BINARY);
        m.put("bool", Types.BOOLEAN);
        m.put("bit", Types.BIT);
        m.put("date", Types.DATE);
        m.put("time", Types.TIME);
        m.put("timetz", Types.TIME);
        m.put("timestamp", Types.TIMESTAMP);
        m.put("timestamptz", Types.TIMESTAMP);
        pgGenericTypeNameToSQLTypeMap = Collections.unmodifiableMap(m);
    }

    private static final Map<String, String> pgGenericTypeNameAliasMap;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("smallint", "int2");
        m.put("integer", "int4");
        m.put("int", "int4");
        m.put("bigint", "int8");
        m.put("real", "float4");
        m.put("float", "float8");
        m.put("double precision", "float8");
        m.put("boolean", "bool");
        m.put("decimal", "numeric");
        m.put("character verrying", "varchar");
        m.put("char", "bpchar");
        m.put("character", "bpchar");
        pgGenericTypeNameAliasMap = Collections.unmodifiableMap(m);
    }

    /**
     * A simple method to get an appropriate java.sql.types.* value for the given PostgreSQL type name
     *
     * @param   typeName
     *
     * @return  SQL type
     */
    public static final int getSQLType(final String typeName) {
        String trimmedTypeName = typeName.trim().toLowerCase(Locale.US);
        final Integer n = pgGenericTypeNameToSQLTypeMap.get(trimmedTypeName);
        if (n == null) {

            // look up the alias
            trimmedTypeName = pgGenericTypeNameAliasMap.get(trimmedTypeName);
            if (trimmedTypeName != null) {
                return pgGenericTypeNameToSQLTypeMap.get(trimmedTypeName);
            }

            return Types.OTHER;
        } else {
            return n;
        }
    }

    private static final Map<Class<?>, String> javaGenericClassToPgTypeNameMap;

    static {
        Map<Class<?>, String> m = new HashMap<Class<?>, String>();
        m.put(short.class, "int2");
        m.put(Short.class, "int2");
        m.put(int.class, "int4");
        m.put(Integer.class, "int4");
        m.put(long.class, "int8");
        m.put(Long.class, "int8");
        m.put(float.class, "float4");
        m.put(Float.class, "float4");
        m.put(double.class, "float8");
        m.put(Double.class, "float8");
        m.put(char.class, "bpchar");
        m.put(Character.class, "bpchar");
        m.put(String.class, "text");
        m.put(boolean.class, "bool");
        m.put(Boolean.class, "bool");
        m.put(Date.class, "timestamp");
        m.put(java.sql.Date.class, "timestamp");
        m.put(java.sql.Timestamp.class, "timestamp");
        m.put(java.sql.Time.class, "timestamp");
        javaGenericClassToPgTypeNameMap = Collections.unmodifiableMap(m);
    }

    public static String getSQLNameForClass(final Class<?> elementClass) {
        if (elementClass == null) {
            return null;
        }

        String typeName = javaGenericClassToPgTypeNameMap.get(elementClass);
        return typeName;
    }

    public static String camelCaseToUnderScore(final String camelCaseName) {

        if (camelCaseName == null) {
            throw new NullPointerException();
        }

        final int length = camelCaseName.length();
        StringBuilder r = new StringBuilder(length * 2);

        // myFieldName -> my_field_name
        // MyFileName -> my_field_name
        // MyFILEName -> my_file_name
        // too lazy to write a small automata here... so quick and dirty by now
        boolean wasUpper = false;
        for (int i = 0; i < length; i++) {
            char ch = camelCaseName.charAt(i);

            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    if ((!wasUpper) && (ch != '_')) {
                        r.append('_');
                    }
                }

                ch = Character.toLowerCase(ch);
                wasUpper = true;
            } else {
                if (wasUpper) {
                    int p = r.length() - 2;
                    if (p > 1 && r.charAt(p) != '_') {
                        r.insert(p, '_');
                    }
                }

                wasUpper = false;
            }

            r.append(ch);
        }

        return r.toString();
    }

    public static final class PgTypeDataHolder {
        private String typeName;
        private Collection<Object> attributes;

        PgTypeDataHolder(final String typeName, final Collection<Object> attributes) {
            this.typeName = typeName;
            this.attributes = attributes;
        }

        public String getTypeName() {
            return typeName;
        }

        public Collection<Object> getAttributes() {
            return attributes;
        }

    }

    public static final PgTypeDataHolder getObjectAttributesForPgSerialization(final Object obj,
            final String typeHint) {
        if (obj == null) {
            throw new NullPointerException();
        }

        String typeName = null;
        Class<?> clazz = obj.getClass();
        if (clazz.isPrimitive() || clazz.isArray()) {
            throw new IllegalArgumentException("Passed object should be a class with parameters");
        }

        DatabaseType databaseType = clazz.getAnnotation(DatabaseType.class);
        if (databaseType != null) {
            typeName = databaseType.name();
        }

        if (typeName == null || typeName.isEmpty()) {

            // if no annotation is given use the typehint parameter
            typeName = typeHint;
        }

        if (typeName == null || typeName.isEmpty()) {

            // fill the name with de-CamelCased name if we could not get it from the annotation
            typeName = camelCaseToUnderScore(clazz.getSimpleName());
        }

        List<Object> resultList = null;
        TreeMap<Integer, Object> resultPositionMap = null;
        TreeMap<Integer, Object> resultNameMap = null;

        Field[] fields = obj.getClass().getDeclaredFields();

        // Hacky: sort fields alphabetically as class fields' order is undefined
        // http://stackoverflow.com/questions/1097807/java-reflection-is-the-order-of-class-fields-and-methods-standardized
        Arrays.sort(fields, new Comparator<Field>() {

                @Override
                public int compare(final Field a, final Field b) {
                    return a.getName().compareTo(b.getName());
                }

            });

        for (Field f : fields) {
            DatabaseField annotation = f.getAnnotation(DatabaseField.class);
            if (annotation != null) {
                if (!f.isAccessible()) {
                    f.setAccessible(true);
                }

                Object value;
                try {
                    value = f.get(obj);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Could not read value of field " + f.getName(), e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Could not read value of field " + f.getName(), e);
                }

                int fieldPosition = annotation.position();
                if (fieldPosition > 0) {
                    if (resultPositionMap == null) {
                        resultPositionMap = new TreeMap<Integer, Object>();
                    }

                    resultPositionMap.put(fieldPosition, value);
                } else {
                    if (resultList == null) {
                        resultList = new ArrayList<Object>();
                    }

                    resultList.add(value);
                }
            }
        }

        int fieldsWithDefinedPositions = resultPositionMap == null ? 0 : resultPositionMap.size();
        int fieldsWithUndefinedPositions = resultList == null ? 0 : resultList.size();
        if (fieldsWithDefinedPositions > 0 && fieldsWithUndefinedPositions > 0) {
            throw new IllegalArgumentException("Class " + clazz.getName()
                    + " should have all its database related fields marked with correct positions");
        }

        if (fieldsWithDefinedPositions > 0) {
            return new PgTypeDataHolder(typeName, Collections.unmodifiableCollection(resultPositionMap.values()));
        } else {
            return new PgTypeDataHolder(typeName, Collections.unmodifiableCollection(resultList));
        }
    }

    /**
     * Serialize an object into a PostgreSQL string.
     *
     * @param  o  object to be serialized
     */
    public static final String toPgString(final Object o) {
        if (o == null) {
            return "NULL";
        }

        StringBuilder sb = new StringBuilder();
        final Class<?> clazz = o.getClass();
        if (clazz == Boolean.TYPE || clazz == Boolean.class) {
            sb.append(((Boolean) o) ? 't' : 'f');
        } else if (clazz.isPrimitive() || o instanceof Number) {
            sb.append(o);
        } else if (o instanceof PGobject || o instanceof java.sql.Array || o instanceof CharSequence
                || o instanceof Character || clazz == Character.TYPE) {
            sb.append(o.toString());
        } else if (clazz.isArray()) {
            final Class<?> componentClazz = clazz.getComponentType();
            if (componentClazz.isPrimitive()) {

                // we are fucked up again with the primitive arrays
                // cast it into a string list
                final int l = Array.getLength(o);
                final List<String> stringList = new ArrayList<String>(l);
                for (int i = 0; i < l; i++) {
                    stringList.add(String.valueOf(Array.get(o, i)));
                }

                sb.append(PgArray.ARRAY(stringList).toString());
            } else {
                sb.append(PgArray.ARRAY((Object[]) o).toString());
            }
        } else if (o instanceof Map) {
            final Map<Object, Object> map = (Map<Object, Object>) o;
            sb.append(HStore.serialize(map));
        } else if (o instanceof Collection) {
            sb.append(PgArray.ARRAY((Collection<?>) o).toString());
        } else {

            // we do not know what to do with this object,
            // try to extract the attributes marked as @DatabaseField and pack it as a ROW
            // here we do not need to know the name of the PG type
            try {
                sb.append(asPGobject(o).toString());
            } catch (SQLException e) {
                throw new IllegalArgumentException("Could not serialize object of class " + clazz.getName(), e);
            }
        }

        return sb.toString();
    }

    public static final PgRow asPGobject(final Object o) throws SQLException {
        return new PgRow(getObjectAttributesForPgSerialization(o, null));
    }

    public static final PgRow asPGobject(final Object o, final String typeHint) throws SQLException {
        return new PgRow(getObjectAttributesForPgSerialization(o, typeHint));
    }

}