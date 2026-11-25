package com.taskflow.model;
public record Subtask(long id,long taskId,String title,boolean completed,int position) {}
