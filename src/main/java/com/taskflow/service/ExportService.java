package com.taskflow.service;

import com.google.gson.*;
import com.taskflow.dao.JdbcDao;
import com.taskflow.exception.ValidationException;
import com.taskflow.model.*;
import com.taskflow.model.enums.Priority;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.io.IOException;

/** Portable project backup: never exports password hashes or session tokens. Import always creates copies. */
public final class ExportService {
    private final JdbcDao db; private final ProjectService projects; private final TaskService tasks; private final AuthService auth;
    private final Gson gson=new GsonBuilder().setPrettyPrinting().create();
    public ExportService(JdbcDao db,ProjectService p,TaskService t,AuthService a) { this.db=db; projects=p; tasks=t; auth=a; }
    private JsonArray rows(List<Map<String,Object>> rows) {
        JsonArray array=new JsonArray();
        for(var row:rows) {
            JsonObject obj=new JsonObject();
            row.forEach((k,v)->{ if(v==null) obj.add(k,JsonNull.INSTANCE); else if(v instanceof Number n) obj.addProperty(k,n); else if(v instanceof Boolean b) obj.addProperty(k,b); else obj.addProperty(k,v.toString()); });
            array.add(obj);
        }
        return array;
    }
    public void exportJson(Path path) throws IOException {
        JsonObject root=db.transaction(()->{
            JsonObject out=new JsonObject(); out.addProperty("format","taskflow-1"); JsonArray all=new JsonArray();
            for(Project p:projects.list()) {
                JsonObject project=rows(db.query("SELECT name,description,color,icon,start_date,end_date,is_archived FROM projects WHERE id=?",p.id())).get(0).getAsJsonObject();
                project.add("members",rows(db.query("SELECT u.username,m.role FROM project_members m JOIN users u ON u.id=m.user_id WHERE m.project_id=?",p.id())));
                project.add("columns",rows(db.query("SELECT * FROM board_columns WHERE project_id=? ORDER BY position,id",p.id())));
                project.add("tags",rows(db.query("SELECT * FROM tags WHERE project_id=?",p.id())));
                JsonArray taskArray=new JsonArray();
                for(Task t:tasks.project(p.id())) {
                    JsonObject task=rows(db.query("SELECT t.*,u.username assignee_username FROM tasks t LEFT JOIN users u ON u.id=t.assignee_id WHERE t.id=?",t.id())).get(0).getAsJsonObject();
                    task.add("subtasks",rows(tasks.subtasks(t.id())));
                    task.add("comments",rows(db.query("SELECT c.content,c.created_at,u.username FROM comments c JOIN users u ON u.id=c.user_id WHERE c.task_id=?",t.id())));
                    task.add("activity",rows(db.query("SELECT a.action,a.created_at,u.username FROM activity_logs a LEFT JOIN users u ON u.id=a.user_id WHERE a.task_id=?",t.id())));
                    task.add("tag_ids",rows(db.query("SELECT tag_id FROM task_tags WHERE task_id=?",t.id())));
                    taskArray.add(task);
                }
                project.add("tasks",taskArray); all.add(project);
            }
            out.add("projects",all); return out;
        });
        Files.writeString(path,gson.toJson(root),java.nio.charset.StandardCharsets.UTF_8);
    }
    private static String s(JsonObject o,String key) { return !o.has(key)||o.get(key).isJsonNull()?"":o.get(key).getAsString(); }
    private static long n(JsonObject o,String key) { return o.get(key).getAsLong(); }
    private static boolean b(JsonObject o,String key) { String v=s(o,key); return v.equalsIgnoreCase("true")||v.equals("1"); }
    private static LocalDate date(JsonObject o,String key) { return s(o,key).isBlank()?null:LocalDate.parse(s(o,key)); }
    private static LocalDateTime time(JsonObject o,String key) { return s(o,key).isBlank()?null:LocalDateTime.parse(s(o,key).replace(' ','T')); }
    private Long user(String username) { if(username.isBlank()) return null; var r=db.query("SELECT id FROM users WHERE username=?",username); return r.isEmpty()?null:JdbcDao.number(r.getFirst(),"id"); }
    public int importJson(Path path) throws IOException {
        if(Files.size(path)>20_000_000) throw new ValidationException("File sao lưu tối đa 20 MB.");
        JsonObject root;
        try { root=JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
        catch(RuntimeException e) { throw new ValidationException("File JSON không hợp lệ."); }
        if(!"taskflow-1".equals(s(root,"format"))||!root.has("projects")) throw new ValidationException("Không đúng định dạng sao lưu TaskFlow.");
        try { return db.transaction(()->{
            int count=0;
            for(JsonElement element:root.getAsJsonArray("projects")) {
                JsonObject p=element.getAsJsonObject();
                String name=s(p,"name"); if(name.length()>135) name=name.substring(0,135);
                long pid=projects.save(null,name+" (khôi phục)",s(p,"description"),s(p,"color"),s(p,"icon"),date(p,"start_date"),date(p,"end_date"),false);
                db.update("DELETE FROM board_columns WHERE project_id=?",pid);
                for(JsonElement me:p.getAsJsonArray("members")) {
                    Long uid=user(s(me.getAsJsonObject(),"username"));
                    if(uid!=null&&uid!=auth.user().id()) db.update("INSERT INTO project_members(project_id,user_id,role) VALUES(?,?,'MEMBER')",pid,uid);
                }
                Map<Long,Long> columns=new HashMap<>(),tags=new HashMap<>();
                for(JsonElement ce:p.getAsJsonArray("columns")) {
                    JsonObject c=ce.getAsJsonObject();
                    columns.put(n(c,"id"),projects.column(null,pid,s(c,"name"),(int)n(c,"wip_limit"),b(c,"is_done_column")));
                }
                if(columns.isEmpty()) throw new ValidationException("Bản sao lưu thiếu cột.");
                for(JsonElement te:p.getAsJsonArray("tags")) {
                    JsonObject tag=te.getAsJsonObject();
                    tags.put(n(tag,"id"),db.insert("INSERT INTO tags(project_id,name,color) VALUES(?,?,?)",pid,s(tag,"name"),s(tag,"color")));
                }
                for(JsonElement te:p.getAsJsonArray("tasks")) {
                    JsonObject t=te.getAsJsonObject(); Long column=columns.get(n(t,"column_id"));
                    if(column==null) throw new ValidationException("Task tham chiếu cột không hợp lệ.");
                    Long assignee=user(s(t,"assignee_username"));
                    if(assignee!=null) { long uid=assignee; if(projects.members(pid).stream().noneMatch(u->u.id()==uid)) assignee=null; }
                    long tid=tasks.save(null,pid,column,s(t,"title"),s(t,"description"),Priority.valueOf(s(t,"priority")),time(t,"deadline"),assignee,"");
                    boolean done=projects.columns(pid).stream().anyMatch(c->c.id()==column&&c.done());
                    LocalDateTime completed=done?time(t,"completed_at"):null;
                    db.update("UPDATE tasks SET position=?,created_at=?,updated_at=?,completed_at=? WHERE id=?",n(t,"position"),time(t,"created_at"),time(t,"updated_at"),done&&completed==null?LocalDateTime.now():completed,tid);
                    for(JsonElement se:t.getAsJsonArray("subtasks")) { JsonObject sub=se.getAsJsonObject(); tasks.subtask(tid,null,s(sub,"title"),b(sub,"is_completed")); }
                    for(JsonElement tag:t.getAsJsonArray("tag_ids")) { Long tagId=tags.get(n(tag.getAsJsonObject(),"tag_id")); if(tagId==null) throw new ValidationException("Tag không hợp lệ."); db.update("INSERT INTO task_tags(task_id,tag_id) VALUES(?,?)",tid,tagId); }
                    for(JsonElement cm:t.getAsJsonArray("comments")) {
                        JsonObject c=cm.getAsJsonObject(); Long uid=user(s(c,"username"));
                        db.insert("INSERT INTO comments(task_id,user_id,content,created_at) VALUES(?,?,?,?)",tid,uid==null?auth.user().id():uid,s(c,"content"),time(c,"created_at"));
                    }
                    db.update("DELETE FROM activity_logs WHERE task_id=?",tid);
                    for(JsonElement ae:t.getAsJsonArray("activity")) { JsonObject a=ae.getAsJsonObject(); db.insert("INSERT INTO activity_logs(task_id,user_id,action,created_at) VALUES(?,?,?,?)",tid,user(s(a,"username")),s(a,"action"),time(a,"created_at")); }
                    tasks.log(tid,"Khôi phục từ JSON");
                }
                if(b(p,"is_archived")) db.update("UPDATE projects SET is_archived=TRUE WHERE id=?",pid);
                count++;
            }
            return count;
        }); } catch(RuntimeException e) { if(e instanceof ValidationException v) throw v; throw new ValidationException("Không thể khôi phục: dữ liệu không hợp lệ. Toàn bộ thao tác đã rollback."); }
    }
}
