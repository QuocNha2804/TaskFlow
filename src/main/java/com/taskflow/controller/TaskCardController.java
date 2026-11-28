package com.taskflow.controller;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.ui.component.AvatarBadge;
import com.taskflow.ui.*;
import com.taskflow.util.DateUtil;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.input.*;
public final class TaskCardController {
    private TaskCardController() {}
    public static VBox card(Task t) {
        Label title=new Label(t.title()); title.setWrapText(true); title.getStyleClass().add("card-title");
        Label priority=new Label(t.priority().name()); priority.getStyleClass().addAll("badge",t.priority().name().toLowerCase());
        VBox box=(VBox)ViewManager.load("component/task_card"); box.getChildren().addAll(priority,title);
        String stripe=switch(t.priority()){case LOW->"#3f9e77";case MEDIUM->"#608fea";case HIGH->"#d78a20";case URGENT->"#e05b60";};
        box.setStyle("-fx-border-width: 3 1 1 1; -fx-border-color: "+stripe+" -color-border-default -color-border-default -color-border-default;");
        if(!t.tags().isBlank()) { javafx.scene.layout.FlowPane pills=new javafx.scene.layout.FlowPane(5,5); for(String name:t.tags().split(",")){Label tag=Ui.muted(name.strip());tag.getStyleClass().add("tag");pills.getChildren().add(tag);} box.getChildren().add(pills); }
        if(t.deadline()!=null) { Label deadline=new Label("◷ "+DateUtil.format(t.deadline())); deadline.getStyleClass().add(t.overdue()?"overdue":t.completedAt()==null&&t.deadline().isBefore(java.time.LocalDateTime.now().plusHours(24))?"due-soon":"muted"); box.getChildren().add(deadline); }
        javafx.scene.layout.HBox footer=Ui.row(Ui.muted("☑ "+t.subtaskDone()+"/"+t.subtaskCount()+"    ◌ "+t.commentCount()));
        javafx.scene.layout.Region spacer=new javafx.scene.layout.Region();footer.getChildren().add(spacer);javafx.scene.layout.HBox.setHgrow(spacer,javafx.scene.layout.Priority.ALWAYS);
        if(t.assigneeId()!=null){AvatarBadge avatar=new AvatarBadge(new User(t.assigneeId(),"","",t.assigneeName(),t.assigneeColor(),t.assigneeAvatarUrl()));javafx.scene.control.Tooltip.install(avatar,new javafx.scene.control.Tooltip(t.assigneeName()));footer.getChildren().add(avatar);}else footer.getChildren().add(Ui.muted("Chưa giao"));box.getChildren().add(footer);
        box.setOnMouseClicked(e->{if(e.getClickCount()==2) TaskDialogController.open(t.id(),t.projectId(),null,null);});
        box.setOnDragDetected(e->{ Dragboard d=box.startDragAndDrop(TransferMode.MOVE); ClipboardContent c=new ClipboardContent(); c.putString("taskflow:"+t.id()); d.setContent(c); e.consume(); });
        return box;
    }
}
