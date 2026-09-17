package edu.deploylab;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Db {
    public final JdbcTemplate jdbc;
    public Db(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Map<String,Object> one(String sql, Object... args) {
        var rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) throw ApiException.missing();
        return rows.getFirst();
    }
    public boolean exists(String sql, Object... args) { return !jdbc.queryForList(sql, args).isEmpty(); }
    public static UUID id(Map<String,Object> row, String key) { return UUID.fromString(row.get(key).toString()); }
}
