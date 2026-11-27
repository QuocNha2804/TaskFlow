package com.taskflow.service;
import com.taskflow.model.Task;
import java.time.*;
import java.util.*;
public final class AnalyticsService {
    public record Metrics(long today,long overdue,long completedWeek,double averageHours,double onTimePercent) {}
    public Metrics metrics(List<Task> tasks) {
        LocalDate today=LocalDate.now(); LocalDate monday=today.with(java.time.DayOfWeek.MONDAY);
        var done=tasks.stream().filter(t->t.completedAt()!=null).toList();
        var dated=done.stream().filter(t->t.deadline()!=null).toList();
        return new Metrics(tasks.stream().filter(t->t.deadline()!=null&&t.deadline().toLocalDate().equals(today)&&t.completedAt()==null).count(),tasks.stream().filter(Task::overdue).count(),done.stream().filter(t->!t.completedAt().toLocalDate().isBefore(monday)&&!t.completedAt().toLocalDate().isAfter(today)).count(),done.stream().mapToLong(t->Duration.between(t.createdAt(),t.completedAt()).toMinutes()).average().orElse(0)/60.0,dated.isEmpty()?0:100.0*dated.stream().filter(t->!t.completedAt().isAfter(t.deadline())).count()/dated.size());
    }
    public Map<LocalDate,Long> completions(List<Task> tasks,int days) {
        Map<LocalDate,Long> result=new LinkedHashMap<>();
        for(int i=days-1;i>=0;i--) { LocalDate date=LocalDate.now().minusDays(i); result.put(date,tasks.stream().filter(t->t.completedAt()!=null&&t.completedAt().toLocalDate().equals(date)).count()); }
        return result;
    }
}
