package com.niocess.perflab.client.db;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DbDriverConverter implements Converter<String, DbDriver> {

    @Override
    public DbDriver convert(String source) {
        try {
            return DbDriver.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(source);
        }
    }
}
