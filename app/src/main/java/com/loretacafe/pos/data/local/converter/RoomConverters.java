package com.loretacafe.pos.data.local.converter;

import android.text.TextUtils;

import androidx.room.TypeConverter;

import com.loretacafe.pos.data.local.entity.PendingSyncType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class RoomConverters {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @TypeConverter
    public static String fromOffsetDateTime(OffsetDateTime value) {
        return value == null ? null : value.format(FORMATTER);
    }

    @TypeConverter
    public static OffsetDateTime toOffsetDateTime(String value) {
        return TextUtils.isEmpty(value) ? null : OffsetDateTime.parse(value, FORMATTER);
    }

    @TypeConverter
    public static String fromBigDecimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    @TypeConverter
    public static BigDecimal toBigDecimal(String value) {
        return TextUtils.isEmpty(value) ? BigDecimal.ZERO : new BigDecimal(value);
    }

    @TypeConverter
    public static String fromPendingSyncType(PendingSyncType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static PendingSyncType toPendingSyncType(String value) {
        return TextUtils.isEmpty(value) ? null : PendingSyncType.valueOf(value);
    }
}

