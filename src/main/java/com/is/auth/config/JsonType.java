package com.is.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.usertype.UserType;

import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class JsonType implements UserType {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.JAVA_OBJECT};
    }

    @Override
    public Class returnedClass() {
        return Object.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String value = rs.getString(names[0]);
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (IOException e) {
            throw new HibernateException("Error deserializing JSON", e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            try {
                st.setObject(index, objectMapper.writeValueAsString(value), Types.OTHER);
            } catch (IOException e) {
                throw new HibernateException("Error serializing JSON", e);
            }
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            return objectMapper.readValue(json, Object.class);
        } catch (IOException e) {
            throw new HibernateException("Error deep copying JSON", e);
        }
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new HibernateException("Error disassembling JSON", e);
        }
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        try {
            return objectMapper.readValue((String) cached, Object.class);
        } catch (IOException e) {
            throw new HibernateException("Error assembling JSON", e);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
} 