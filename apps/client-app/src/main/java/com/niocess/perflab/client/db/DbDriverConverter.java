package com.niocess.perflab.client.db;

import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DbDriverConverter implements Converter<String, DbDriver> {

    @Override
    public DbDriver convert(String source) {
        try {
            return DbDriver.valueOf(source.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(source);
        }
    }
}
