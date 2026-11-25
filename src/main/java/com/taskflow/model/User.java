package com.taskflow.model;
import java.util.Map;
import static com.taskflow.dao.JdbcDao.*;
public record User(long id,String username,String email,String fullName,String avatarColor,String avatarUrl) {
    public static User from(Map<String,Object> r) { return new User(number(r,"id"),string(r,"username"),string(r,"email"),string(r,"full_name"),string(r,"avatar_color"),string(r,"avatar_url")); }
    @Override public String toString() { return fullName+" (@"+username+")"; }
}
