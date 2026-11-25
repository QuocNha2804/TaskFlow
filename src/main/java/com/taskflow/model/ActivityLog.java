package com.taskflow.model;
import java.time.LocalDateTime;
public record ActivityLog(long id,long taskId,Long userId,String action,LocalDateTime createdAt) {}
