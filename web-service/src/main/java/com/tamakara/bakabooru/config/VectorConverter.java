package com.tamakara.bakabooru.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.stream.Collectors;

/** JPA double[] 与 PostgreSQL vector 字符串之间的转换器。 */
@Converter
public class VectorConverter implements AttributeConverter<double[], String> {

    @Override
    public String convertToDatabaseColumn(double[] attribute) {
        if (attribute == null) {
            return null;
        }
        // PostgreSQL vector 格式为 [0.1,0.2,0.3,...]。
        return "[" + Arrays.stream(attribute)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    @Override
    public double[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        String content = dbData.substring(1, dbData.length() - 1);
        if (content.isEmpty()) {
            return new double[0];
        }

        return Arrays.stream(content.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
