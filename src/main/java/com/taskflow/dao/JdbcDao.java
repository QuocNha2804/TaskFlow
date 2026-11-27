package com.taskflow.dao;

import com.taskflow.exception.AppException;
import javax.sql.DataSource;
import java.sql.*;
import java.time.*;
import java.util.*;

/** All values use PreparedStatement bindings. A transaction pins one connection per thread. */
public final class JdbcDao {
    private final DataSource source;
    private final ThreadLocal<Connection> transaction = new ThreadLocal<>();
    public JdbcDao(DataSource source) { this.source = source; }
    @FunctionalInterface public interface Work<T> { T run() throws Exception; }
    private <T> T connection(SqlWork<T> work) {
        try {
            Connection existing = transaction.get();
            if (existing != null) return work.run(existing);
            try (Connection c = source.getConnection()) { return work.run(c); }
        } catch (SQLException e) { throw new AppException("Thao tác database thất bại. Kiểm tra dữ liệu và kết nối.", e); }
    }
    @FunctionalInterface private interface SqlWork<T> { T run(Connection c) throws SQLException; }
    public <T> T transaction(Work<T> work) {
        if (transaction.get() != null) { try { return work.run(); } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new AppException("Giao dịch thất bại", e); } }
        try (Connection c = source.getConnection()) {
            c.setAutoCommit(false); transaction.set(c);
            try { T result = work.run(); c.commit(); return result; }
            catch (Exception e) { c.rollback(); if (e instanceof RuntimeException r) throw r; throw new AppException("Giao dịch thất bại", e); }
            finally { transaction.remove(); }
        } catch (SQLException e) { throw new AppException("Không thể thực hiện giao dịch", e); }
    }
    private PreparedStatement prepare(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            for (int i=0;i<args.length;i++) {
                Object v=args[i];
                if (v instanceof LocalDateTime d) v=Timestamp.valueOf(d);
                if (v instanceof LocalDate d) v=java.sql.Date.valueOf(d);
                if (v instanceof Enum<?> e) v=e.name();
                s.setObject(i+1,v);
            }
            return s;
        } catch (SQLException e) { s.close(); throw e; }
    }
    public List<Map<String,Object>> query(String sql, Object... args) {
        return connection(c -> {
            try (PreparedStatement s=prepare(c,sql,args); ResultSet rs=s.executeQuery()) {
                List<Map<String,Object>> rows=new ArrayList<>();
                while(rs.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=rs.getMetaData().getColumnCount();i++) row.put(rs.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT),rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        });
    }
    public int update(String sql, Object... args) { return connection(c -> { try(PreparedStatement s=prepare(c,sql,args)) { return s.executeUpdate(); } }); }
    public long insert(String sql, Object... args) { return connection(c -> {
        try(PreparedStatement s=prepare(c,sql,args)) {
            s.executeUpdate(); try(ResultSet r=s.getGeneratedKeys()) { if(r.next()) return r.getLong(1); throw new SQLException("Missing generated key"); }
        }
    }); }
    public static long number(Map<String,Object> r,String key) { Object v=r.get(key); return v==null?0:((Number)v).longValue(); }
    public static String string(Map<String,Object> r,String key) { return Objects.toString(r.get(key),""); }
    public static boolean bool(Map<String,Object> r,String key) { Object v=r.get(key); return v instanceof Boolean b?b:v instanceof Number n&&n.intValue()!=0; }
    public static LocalDateTime time(Map<String,Object> r,String key) { Object v=r.get(key); return v==null?null:v instanceof Timestamp t?t.toLocalDateTime():v instanceof LocalDateTime d?d:LocalDateTime.parse(v.toString().replace(' ','T')); }
    public static LocalDate date(Map<String,Object> r,String key) { Object v=r.get(key); return v==null?null:v instanceof java.sql.Date d?d.toLocalDate():LocalDate.parse(v.toString()); }
}
