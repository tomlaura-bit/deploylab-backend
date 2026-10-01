package edu.deploylab;

import jakarta.persistence.*;
import java.nio.ByteBuffer;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class Db {
    @PersistenceContext private EntityManager entityManager;
    @Transactional(readOnly=true)
    public List<Map<String,Object>> query(String sql,Object... args) {
        Query query=entityManager.createNativeQuery(numbered(sql),Tuple.class);
        bind(query,args);
        @SuppressWarnings("unchecked") List<Tuple> rows=query.getResultList();
        return rows.stream().map(Db::map).toList();
    }
    @Transactional
    public int update(String sql,Object... args) {
        Query query=entityManager.createNativeQuery(numbered(sql));
        bind(query,args);
        return query.executeUpdate();
    }
    @Transactional(readOnly=true)
    public <T> T scalar(String sql,Class<T> type,Object... args) {
        Query query=entityManager.createNativeQuery(numbered(sql));
        bind(query,args);
        Object value=query.getSingleResult();
        if(type.isInstance(value)) return type.cast(value);
        if(value instanceof Number number) {
            if(type==Integer.class) return type.cast(number.intValue());
            if(type==Long.class) return type.cast(number.longValue());
            if(type==Double.class) return type.cast(number.doubleValue());
        }
        return type.cast(value);
    }
    public Map<String,Object> one(String sql, Object... args) {
        var rows=query(sql,args);
        if (rows.isEmpty()) throw ApiException.missing();
        return rows.getFirst();
    }
    public boolean exists(String sql,Object... args){return !query(sql,args).isEmpty();}
    public static UUID id(Map<String,Object> row, String key) { return UUID.fromString(row.get(key).toString()); }
    private static Map<String,Object> map(Tuple tuple) {
        Map<String,Object> result=new LinkedHashMap<>();
        tuple.getElements().forEach(element->result.put(element.getAlias().toLowerCase(Locale.ROOT),normalize(tuple.get(element))));
        return result;
    }
    private static Object normalize(Object value) {
        if(value instanceof byte[] bytes&&bytes.length==16) {
            var buffer=ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(),buffer.getLong());
        }
        return value;
    }
    private static void bind(Query query,Object[] args){for(int i=0;i<args.length;i++) query.setParameter(i+1,args[i]);}
    private static String numbered(String sql) {
        StringBuilder result=new StringBuilder();int parameter=1;
        for(int i=0;i<sql.length();i++) result.append(sql.charAt(i)=='?'?'?'+Integer.toString(parameter++):sql.charAt(i));
        return result.toString();
    }
}
