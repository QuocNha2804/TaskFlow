package com.taskflow.util;
import com.taskflow.model.Task;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;
public final class CsvUtil {
    private CsvUtil() {}
    public static String cell(String value) {
        String text=Objects.toString(value,"");
        String stripped=text.stripLeading();
        if(!stripped.isEmpty()&&"=+@-".indexOf(stripped.charAt(0))>=0) text="'"+text;
        return "\""+text.replace("\"","\"\"")+"\"";
    }
    public static void write(Path path,List<Task> tasks) throws IOException {
        StringBuilder out=new StringBuilder("\uFEFFTitle,Project,Priority,Deadline,Status,Assignee,Checklist,Tags\r\n");
        for(Task t:tasks) out.append(String.join(",",List.of(t.title(),t.projectName(),t.priority().name(),DateUtil.format(t.deadline()),t.columnName(),t.assigneeName(),t.subtaskDone()+"/"+t.subtaskCount(),t.tags()).stream().map(CsvUtil::cell).toList())).append("\r\n");
        Files.writeString(path,out,StandardCharsets.UTF_8);
    }
}
