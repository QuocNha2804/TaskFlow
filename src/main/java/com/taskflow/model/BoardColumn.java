package com.taskflow.model;
import java.util.Map;
import static com.taskflow.dao.JdbcDao.*;
public record BoardColumn(long id,long projectId,String name,int position,int wipLimit,boolean done) {
    public static BoardColumn from(Map<String,Object> r) { return new BoardColumn(number(r,"id"),number(r,"project_id"),string(r,"name"),(int)number(r,"position"),(int)number(r,"wip_limit"),bool(r,"is_done_column")); }
    @Override public String toString() { return name; }
}
