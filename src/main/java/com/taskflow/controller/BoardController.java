package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.model.*;
import com.taskflow.model.enums.Priority;
import com.taskflow.ui.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import java.util.*;

public final class BoardController {
    @FXML private BorderPane root;
    private TextField search,tag; private ComboBox<Priority> priority; private ComboBox<User> assignee;
    private long pid; private HBox columns;
    @FXML public void initialize() {
        MainApp a=MainApp.app; Long selected=MainLayoutController.current.selectedProject;
        if(selected==null) {root.setCenter(Ui.page("Kanban",Ui.muted("Chọn hoặc tạo project từ thanh bên.")));return;} pid=selected;
        Project p=a.projects.require(pid);
        search=new TextField();search.setPromptText("Tìm tiêu đề / mô tả"); tag=new TextField();tag.setPromptText("Lọc tag");
        priority=new ComboBox<>();priority.getItems().add(null);priority.getItems().addAll(Priority.values());priority.setPromptText("Mọi ưu tiên");
        assignee=new ComboBox<>();assignee.getItems().add(null);assignee.getItems().addAll(a.projects.members(pid));assignee.setPromptText("Mọi thành viên");
        root.setTop(Ui.page(p.icon()+"  "+p.name()+(p.archived()?" · Lưu trữ":""),Ui.row(Ui.muted(p.description()),Ui.button("Quản lý project",()->ProjectDialogController.open(p)),Ui.button("+ Cột",()->columnDialog(null))),Ui.row(search,priority,assignee,tag,Ui.button("Xóa lọc",()->{search.clear();tag.clear();priority.setValue(null);assignee.setValue(null);})),Ui.muted("Kéo thả để đổi cột hoặc thứ tự · Nhấp đúp để mở chi tiết")));
        columns=new HBox(16);columns.setPadding(new javafx.geometry.Insets(4,26,26,26));ScrollPane scroll=new ScrollPane(columns);scroll.setFitToHeight(true);root.setCenter(scroll);
        search.textProperty().addListener((o,x,y)->render());tag.textProperty().addListener((o,x,y)->render());priority.valueProperty().addListener((o,x,y)->render());assignee.valueProperty().addListener((o,x,y)->render());render();
    }
    private void render() {
        columns.getChildren().clear(); MainApp a=MainApp.app; var all=a.tasks.project(pid);
        for(BoardColumn c:a.projects.columns(pid)) {
            var full=all.stream().filter(t->t.columnId()==c.id()).toList();
            Label count=new Label(c.name()+"   "+full.size()+(c.wipLimit()>0?" / "+c.wipLimit():""));count.getStyleClass().add("column-title"); if(c.wipLimit()>0&&full.size()>c.wipLimit()) count.getStyleClass().add("overdue");
            VBox list=new VBox(12); list.setMinHeight(150);
            VBox col=new VBox(14,Ui.row(count,Ui.button("⋯",()->columnDialog(c))),list,Ui.button("+ Thêm task",()->TaskDialogController.open(null,pid,c.id(),null)));col.getStyleClass().add("kanban-column");col.setPrefWidth(305);col.setMinWidth(305);
            for(Task t:full) if(matches(t)) {
                VBox card=TaskCardController.card(t); list.getChildren().add(card);
                card.setOnDragOver(e->{if(valid(e)){e.acceptTransferModes(TransferMode.MOVE);e.consume();}});
                card.setOnDragDropped(e->{drop(e,c.id(),full.indexOf(t)+(e.getY()>card.getHeight()/2?1:0));e.consume();});
            }
            if(list.getChildren().isEmpty()) list.getChildren().add(Ui.muted("Chưa có task phù hợp.\nThêm task hoặc thả vào đây."));
            col.setOnDragOver(e->{if(valid(e)){e.acceptTransferModes(TransferMode.MOVE);e.consume();}});
            col.setOnDragDropped(e->drop(e,c.id(),full.size()));columns.getChildren().add(col);
        }
    }
    private boolean matches(Task t) { String q=search.getText().toLowerCase(Locale.ROOT);return (t.title()+" "+t.description()).toLowerCase(Locale.ROOT).contains(q)&&(priority.getValue()==null||priority.getValue()==t.priority())&&(assignee.getValue()==null||Objects.equals(t.assigneeId(),assignee.getValue().id()))&&t.tags().toLowerCase(Locale.ROOT).contains(tag.getText().toLowerCase(Locale.ROOT)); }
    private boolean valid(DragEvent e) {return e.getDragboard().hasString()&&e.getDragboard().getString().matches("taskflow:[0-9]+");}
    private void drop(DragEvent e,long column,int index) {
        if(!valid(e)) return;
        Ui.safe(()->{long id=Long.parseLong(e.getDragboard().getString().substring(9));Task t=MainApp.app.tasks.get(id);if(t.projectId()!=pid) throw new com.taskflow.exception.ValidationException("Task không thuộc project này.");int target=index;int original=MainApp.app.tasks.project(pid).stream().filter(item->item.columnId()==column).map(Task::id).toList().indexOf(id);if(original>=0&&original<index)target--;MainApp.app.tasks.move(id,column,target);e.setDropCompleted(true);render();MainLayoutController.current.refreshSidebar();});e.consume();
    }
    private void columnDialog(BoardColumn c) {
        MainApp a=MainApp.app; TextField name=new TextField(c==null?"":c.name()); Spinner<Integer> wip=new Spinner<>(0,1000,c==null?0:Math.min(1000,c.wipLimit()));CheckBox done=new CheckBox("Cột hoàn thành (Done)");done.setSelected(c!=null&&c.done());
        VBox form=Ui.page("Quản lý cột",Ui.field("Tên cột",name),Ui.field("WIP limit (0 = không giới hạn)",wip),done); javafx.stage.Stage stage=Ui.modal("Cột Kanban",form);
        form.getChildren().add(Ui.button("Lưu",()->{a.projects.column(c==null?null:c.id(),pid,name.getText(),wip.getValue(),done.isSelected());stage.close();MainLayoutController.current.refresh();}));
        if(c!=null)form.getChildren().add(Ui.button("Xóa cột",()->{if(Ui.confirm("Xóa cột trống này?")){a.projects.deleteColumn(pid,c.id());stage.close();render();}}));stage.showAndWait();
    }
}
