package com.taskflow.model;
import java.time.LocalDateTime;
public record Comment(long id,long taskId,long userId,String content,LocalDateTime createdAt) {}
