package top.lucki.lottery.common.constant;

import java.math.BigDecimal;
import java.util.Date;

public enum PermissionColumnType {

    INTEGER("integer", Integer.class),
    STRING("string", String.class),
    DATA("date", Date.class),
    BIGDECIMAL("bigDecimal", BigDecimal.class);

    private String key;
    private Class value;

    PermissionColumnType(String key, Class value) {
        this.key = key;
        this.value = value;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Class getValue() {
        return value;
    }

    public void setValue(Class value) {
        this.value = value;
    }

    public static PermissionColumnType getEnum(String param) {
        PermissionColumnType[] values = PermissionColumnType.values();
        for (PermissionColumnType value : values) {
            if (value.getKey().equals(param)) {
                return value;
            }
        }
        return null;
    }
}
