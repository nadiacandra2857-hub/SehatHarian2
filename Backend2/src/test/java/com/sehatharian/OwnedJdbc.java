package com.sehatharian;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

/** Synthetic database behind JDBC, not a mock of repository authorization. Never opens a socket. */
public class OwnedJdbc {
    public record Command(String sql, Map<Integer,Object> parameters) {}
    public final Map<String,List<Map<String,Object>>> tables = new HashMap<>();
    public final List<Command> commands = new ArrayList<>();
    public boolean failRead, failWrite, zeroWrite;
    public boolean loseCommitAck, failCommitBeforeApply, failRollback, failAfterExecute, rollbackWarning;
    public boolean failStatementClose, failConnectionClose, failRestore, missingKey, invalidKey, failKey;
    public int commits, rollbacks, executions;
    public int failExecutionNumber;
    public Runnable beforeCommit = () -> {};
    public Runnable afterCommit = () -> {};
    public int opens, closedConnections, generated = 1000;
    public Runnable afterQuery = () -> {};
    public Runnable afterWrite = () -> {};
    public List<Map<String,Object>> table(String name) { return tables.computeIfAbsent(name, ignored -> new ArrayList<>()); }
    public void add(String name, Object... pairs) {
        Map<String,Object> row = new HashMap<>();
        for (int i=0;i<pairs.length;i+=2) row.put((String)pairs[i],pairs[i+1]);
        table(name).add(row);
    }
    public List<Map<String,Object>> snapshot(String name) { return table(name).stream().map(r -> new HashMap<>(r)).map(r -> (Map<String,Object>)r).toList(); }
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler);
    }
    private static Object empty(Class<?> type) {
        if (type==boolean.class) return false;
        if (type==int.class) return 0;
        if (type==long.class) return 0L;
        if (type==double.class) return 0.0;
        if (type==float.class) return 0f;
        return null;
    }
    private static Map<String,List<Map<String,Object>>> copy(Map<String,List<Map<String,Object>>> source) {
        Map<String,List<Map<String,Object>>> result=new HashMap<>();
        source.forEach((key,rows)->result.put(key,new ArrayList<>(rows.stream().map(row->(Map<String,Object>)new HashMap<>(row)).toList())));
        return result;
    }
    private final class Tx {
        boolean auto=true;
        Map<String,List<Map<String,Object>>> pending;
        Map<String,List<Map<String,Object>>> view() { return pending==null?tables:pending; }
    }
    public Connection connection() {
        opens++; Tx tx=new Tx();
        return proxy(Connection.class,(p,m,args) -> {
            switch(m.getName()) {
                case "getAutoCommit": return tx.auto;
                case "getWarnings": return rollbackWarning ? new SQLWarning("Synthetic rollback warning") : null;
                case "setAutoCommit":
                    boolean mode=(Boolean)args[0];
                    if(mode && failRestore)throw new SQLException("Synthetic restore failure");
                    if(!mode && tx.auto)tx.pending=copy(tables);
                    tx.auto=mode;return null;
                case "commit":
                    commits++;beforeCommit.run();
                    if(failCommitBeforeApply)throw new SQLException("Synthetic commit failure");
                    if(tx.pending!=null) { tables.clear();tables.putAll(copy(tx.pending));tx.pending=null; }
                    afterCommit.run();
                    if(loseCommitAck)throw new SQLException("Synthetic commit acknowledgement lost");
                    return null;
                case "rollback":
                    rollbacks++;
                    if(failRollback)throw new SQLException("Synthetic rollback acknowledgement lost");
                    tx.pending=null;return null;
                case "close":
                    closedConnections++;
                    if(failConnectionClose)throw new SQLException("Synthetic close failure");
                    tx.pending=null;return null;
                case "prepareStatement": return statement((String)args[0],tx);
                default: return empty(m.getReturnType());
            }
        });
    }
    private PreparedStatement statement(String original,Tx tx) {
        String sql=original.toLowerCase(Locale.ROOT);
        Map<Integer,Object> values=new HashMap<>(); int[] key={0};
        return proxy(PreparedStatement.class,(p,m,args) -> {
            String name=m.getName();
            if (name.startsWith("set")) { values.put((Integer)args[0],name.equals("setNull")?null:args[1]); return null; }
            if(name.equals("close") && failStatementClose)throw new SQLException("Synthetic statement close failure");
            if (name.equals("getGeneratedKeys")) {
                if(failKey)throw new SQLException("Synthetic key failure");
                return result(!missingKey && key[0]>0?List.of(Map.of("1",invalidKey?0:key[0])):List.of());
            }
            if (name.equals("executeQuery")) {
                commands.add(new Command(sql,new HashMap<>(values)));
                if (failRead) throw new SQLException("Synthetic read failure");
                var rows=select(sql,values,tx.view()); afterQuery.run(); return result(rows);
            }
            if (name.equals("executeUpdate")) {
                commands.add(new Command(sql,new HashMap<>(values)));executions++;
                if (failWrite || failExecutionNumber==executions) throw new SQLException("Synthetic write failure");
                if (zeroWrite) return 0;
                int count=mutate(sql,values,key,tx.view()); afterWrite.run();
                if(failAfterExecute)throw new SQLException("Synthetic execute acknowledgement lost");
                return count;
            }
            return empty(m.getReturnType());
        });
    }
    private String tableName(String sql) {
        Matcher m=Pattern.compile("(?:from|into|update)\\s+(\\w+)").matcher(sql);
        if (!m.find()) throw new AssertionError("Unsupported synthetic SQL: "+sql);
        return m.group(1);
    }
    private int index(String sql,int position) { return (int)sql.substring(0,position).chars().filter(c->c=='?').count()+1; }
    private boolean matches(Map<String,Object> row,String sql,Map<Integer,Object> values) {
        int where=sql.indexOf(" where "); if(where<0)return true;
        String condition=sql.substring(where);
        Matcher eq=Pattern.compile("(?:[a-z]\\.)?(\\w+)\\s*=\\s*\\?").matcher(condition);
        while(eq.find()) if(!Objects.equals(row.get(eq.group(1)),values.get(index(sql,where+eq.end()-1))))return false;
        Matcher between=Pattern.compile("(\\w+) between \\? and \\?").matcher(condition);
        if(between.find()) {
            Object value=row.get(between.group(1)); int n=index(sql,where+between.start()+between.group(1).length()+9);
            if(value==null)return false;
            String date=value.toString(), start=values.get(n).toString(),end=values.get(n+1).toString();
            if(date.compareTo(start)<0||date.compareTo(end)>0)return false;
        }
        if(condition.contains("lower(email)")) return Objects.toString(row.get("email"),"").equalsIgnoreCase((String)values.get(1));
        return true;
    }
    private List<Map<String,Object>> select(String sql,Map<Integer,Object> values,Map<String,List<Map<String,Object>>> store) {
        var filtered=store.computeIfAbsent(tableName(sql),ignored->new ArrayList<>()).stream().filter(r->matches(r,sql,values)).toList();
        if(sql.contains("sum(")||sql.contains("count(")) {
            int amount=0,done=0;
            for(var row:filtered) {
                if("selesai".equals(row.get("status")))done++;
                if(sql.contains("sum(jumlah_ml)"))amount+=((Number)row.get("jumlah_ml")).intValue();
                if(sql.contains("sum(durasi_menit)"))amount+=((Number)row.get("durasi_menit")).intValue();
            }
            return List.of(Map.of("1",sql.contains("count(")?filtered.size():amount,"total",filtered.size(),"selesai",done,"total_ml",amount,"total_menit",amount));
        }
        List<Map<String,Object>> rows=new ArrayList<>();
        for(var source:filtered) {
            Map<String,Object> row=new HashMap<>(source);
            if(sql.contains("join tantangan")) {
                var item=store.getOrDefault("tantangan",List.of()).stream().filter(t->Objects.equals(t.get("id"),row.get("tantangan_id"))).findFirst().orElse(Map.of());
                row.put("tantangan_judul",item.get("judul"));row.put("tantangan_deskripsi",item.get("deskripsi"));row.put("tantangan_kategori",item.get("kategori"));
            }
            rows.add(row);
        }
        if(sql.contains("desc")) rows.sort((a,b)->Integer.compare(((Number)b.getOrDefault("id",0)).intValue(),((Number)a.getOrDefault("id",0)).intValue()));
        Matcher limit=Pattern.compile("limit (\\d+)").matcher(sql);
        if(limit.find()&&rows.size()>Integer.parseInt(limit.group(1)))return rows.subList(0,Integer.parseInt(limit.group(1)));
        return rows;
    }
    private int mutate(String sql,Map<Integer,Object> values,int[] key,Map<String,List<Map<String,Object>>> store) {
        var table=store.computeIfAbsent(tableName(sql),ignored->new ArrayList<>());
        if(sql.startsWith("insert")) {
            String[] columns=sql.substring(sql.indexOf('(')+1,sql.indexOf(')')).split(",");
            Map<String,Object> row=new HashMap<>();
            for(int i=0;i<columns.length;i++)row.put(columns[i].trim(),values.get(i+1));
            row.put("id",++generated);table.add(row);key[0]=generated;return 1;
        }
        if(sql.startsWith("delete")) { int before=table.size();table.removeIf(r->matches(r,sql,values));return before-table.size(); }
        if(sql.startsWith("update")) {
            int count=0;
            for(var row:table) if(matches(row,sql,values)) {
                Matcher set=Pattern.compile("(\\w+)\\s*=\\s*\\?").matcher(sql.substring(0,sql.indexOf(" where ")));
                int i=1;while(set.find())row.put(set.group(1),values.get(i++));count++;
            }
            return count;
        }
        throw new AssertionError("Unsupported mutation");
    }
    private ResultSet result(List<Map<String,Object>> rows) {
        int[] i={-1}; boolean[] wasNull={false};
        return proxy(ResultSet.class,(p,m,args)-> {
            String name=m.getName();
            if(name.equals("next"))return ++i[0]<rows.size();
            if(name.equals("wasNull"))return wasNull[0];
            if(name.startsWith("get")) {
                Object value=rows.get(i[0]).get(args[0].toString());wasNull[0]=value==null;
                if(name.equals("getInt"))return value==null?0:((Number)value).intValue();
                if(name.equals("getDouble"))return value==null?0.0:((Number)value).doubleValue();
                if(name.equals("getBoolean"))return value instanceof Boolean b?b:value instanceof Number n&&n.intValue()!=0;
                if(name.equals("getString"))return value==null?null:value.toString();
                return value;
            }
            return empty(m.getReturnType());
        });
    }
}
