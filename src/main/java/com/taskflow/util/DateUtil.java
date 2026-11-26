package com.taskflow.util;
import java.time.*;
import java.time.format.DateTimeFormatter;
public final class DateUtil {
    private DateUtil() {}
    public static String format(LocalDateTime value) { return value==null?"—":value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    public static LocalDateTime deadline(LocalDate date,String time) { return date==null?null:date.atTime(LocalTime.parse(time.isBlank()?"23:59":time)); }
}
