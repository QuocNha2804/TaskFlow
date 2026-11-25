package com.taskflow.model;
import java.time.LocalDate;
import java.util.Map;
import static com.taskflow.dao.JdbcDao.*;
public record Project(long id,String name,String description,String color,String icon,LocalDate startDate,LocalDate endDate,long ownerId,boolean archived) {
    public static Project from(Map<String,Object> r) { return new Project(number(r,"id"),string(r,"name"),string(r,"description"),string(r,"color"),string(r,"icon"),date(r,"start_date"),date(r,"end_date"),number(r,"owner_id"),bool(r,"is_archived")); }
    @Override public String toString() { return name+(archived?" (Lưu trữ)":""); }
}
