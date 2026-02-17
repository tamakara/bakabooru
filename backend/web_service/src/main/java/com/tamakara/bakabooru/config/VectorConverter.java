package com.tamakara.bakabooru.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * JPA 转换器：将 double[] 转换为 PostgreSQL vector 类型的字符串格式
 * PostgreSQL vector 格式: [0.1,0.2,0.3,...]
 */
@Converter
public class VectorConverter implements AttributeConverter<double[], String> {

    @Override
    public String convertToDatabaseColumn(double[] attribute) {
        if (attribute == null) {
            return null;
        }
        // 格式化为 PostgreSQL vector 字符串格式: [0.1,0.2,0.3,...]
        return "[" + Arrays.stream(attribute)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    @Override
    public double[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        // 解析 PostgreSQL vector 格式: [0.1,0.2,0.3,...]
        String content = dbData.substring(1, dbData.length() - 1); // 移除 [ 和 ]
        if (content.isEmpty()) {
            return new double[0];
        }

        return Arrays.stream(content.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
