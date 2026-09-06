package com.tamakara.bakabooru.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * JPA 鏉烆剚宕查崳顭掔窗鐏?double[] 鏉烆剚宕叉稉?PostgreSQL vector 缁鐎烽惃鍕摟缁楋缚瑕嗛弽鐓庣础
 * PostgreSQL vector 閺嶇厧绱? [0.1,0.2,0.3,...]
 */
@Converter
public class VectorConverter implements AttributeConverter<double[], String> {

    @Override
    public String convertToDatabaseColumn(double[] attribute) {
        if (attribute == null) {
            return null;
        }
        // 閺嶇厧绱￠崠鏍﹁礋 PostgreSQL vector 鐎涙顑佹稉鍙夌壐瀵? [0.1,0.2,0.3,...]
        return "[" + Arrays.stream(attribute)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(",")) + "]";
    }

    @Override
    public double[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        // 鐟欙絾鐎?PostgreSQL vector 閺嶇厧绱? [0.1,0.2,0.3,...]
        String content = dbData.substring(1, dbData.length() - 1); // 缁夊娅?[ 閸?]
        if (content.isEmpty()) {
            return new double[0];
        }

        return Arrays.stream(content.split(","))
                .mapToDouble(Double::parseDouble)
                .toArray();
    }
}
