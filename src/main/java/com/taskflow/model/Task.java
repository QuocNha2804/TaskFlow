package com.taskflow.model;
import com.taskflow.model.enums.Priority;
import java.time.LocalDateTime;
import java.util.Map;
import static com.taskflow.dao.JdbcDao.*;
public record Task(long id,long projectId,long columnId,String title,String description,Priority priority,int position,LocalDateTime deadline,Long assigneeId,LocalDateTime createdAt,LocalDateTime updatedAt,LocalDateTime completedAt,String projectName,String columnName,String assigneeName,String assigneeColor,String assigneeAvatarUrl,String tags,int subtaskCount,int subtaskDone,int commentCount) {
    public static Task from(Map<String,Object> r) { return new Task(number(r,"id"),number(r,"project_id"),number(r,"column_id"),string(r,"title"),string(r,"description"),Priority.valueOf(string(r,"priority")),(int)number(r,"position"),time(r,"deadline"),r.get("assignee_id")==null?null:number(r,"assignee_id"),time(r,"created_at"),time(r,"updated_at"),time(r,"completed_at"),string(r,"project_name"),string(r,"column_name"),string(r,"assignee_name"),string(r,"assignee_color"),string(r,"assignee_avatar_url"),string(r,"tags"),(int)number(r,"subtask_count"),(int)number(r,"subtask_done"),(int)number(r,"comment_count")); }
    public boolean overdue() { return completedAt==null&&deadline!=null&&deadline.isBefore(LocalDateTime.now()); }
    @Override public String toString() { return title+" · "+projectName; }
}
