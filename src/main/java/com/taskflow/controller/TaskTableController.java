package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.model.*;
import com.taskflow.model.enums.Priority;
import com.taskflow.ui.*;
import com.taskflow.util.*;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.*;
import javafx.collections.transformation.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;
import java.util.function.Function;

public final class TaskTableController {
    @FXML private BorderPane root;
    @FXML public void initialize(){
        MainApp a=MainApp.app;TextField search=new TextField();search.setPromptText("Tìm công việc…");ComboBox<Project> project=new ComboBox<>();project.getItems().add(null);project.getItems().addAll(a.projects.list());project.setPromptText("Mọi project");ComboBox<Priority> priority=new ComboBox<>();priority.getItems().add(null);priority.getItems().addAll(Priority.values());priority.setPromptText("Mọi ưu tiên");ComboBox<String> status=new ComboBox<>();status.getItems().add(null);status.getItems().addAll(a.tasks.list().stream().map(Task::columnName).distinct().toList());status.setPromptText("Mọi trạng thái");
        TableView<Task> table=new TableView<>();FilteredList<Task> filtered=new FilteredList<>(FXCollections.observableArrayList(a.tasks.list()));SortedList<Task> sorted=new SortedList<>(filtered);sorted.comparatorProperty().bind(table.comparatorProperty());table.setItems(sorted);
        add(table,"Tiêu đề",Task::title);add(table,"Project",Task::projectName);add(table,"Ưu tiên",Task::priority);add(table,"Deadline",Task::deadline);add(table,"Trạng thái",Task::columnName);add(table,"Assignee",Task::assigneeName);add(table,"Checklist",t->t.subtaskDone()+"/"+t.subtaskCount());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);table.setPlaceholder(Ui.muted("Không có công việc phù hợp."));
        Runnable filter=()->filtered.setPredicate(t->(t.title()+" "+t.description()).toLowerCase(Locale.ROOT).contains(search.getText().toLowerCase(Locale.ROOT))&&(project.getValue()==null||project.getValue().id()==t.projectId())&&(priority.getValue()==null||priority.getValue()==t.priority())&&(status.getValue()==null||status.getValue().equals(t.columnName())));
        search.textProperty().addListener((o,x,y)->filter.run());project.setOnAction(e->filter.run());priority.setOnAction(e->filter.run());status.setOnAction(e->filter.run());
        table.setRowFactory(v->{TableRow<Task> row=new TableRow<>();row.itemProperty().addListener((o,x,t)->row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("overdue"),t!=null&&t.overdue()));row.setOnMouseClicked(e->{if(e.getClickCount()==2&&!row.isEmpty()){Task t=row.getItem();TaskDialogController.open(t.id(),t.projectId(),null,null);}});
            row.setOnContextMenuRequested(e->{if(row.isEmpty())return;Task t=row.getItem();ContextMenu menu=new ContextMenu();MenuItem open=new MenuItem("Mở chi tiết");open.setOnAction(x->TaskDialogController.open(t.id(),t.projectId(),null,null));menu.getItems().add(open);Menu states=new Menu("Đổi trạng thái");for(var c:a.projects.columns(t.projectId())){MenuItem item=new MenuItem(c.name());item.setOnAction(x->Ui.safe(()->{a.tasks.move(t.id(),c.id(),Integer.MAX_VALUE);MainLayoutController.current.refresh();}));states.getItems().add(item);}menu.getItems().add(states);Menu priorities=new Menu("Đổi ưu tiên");for(Priority p:Priority.values()){MenuItem item=new MenuItem(p.name());item.setOnAction(x->Ui.safe(()->{a.tasks.priority(t.id(),p);MainLayoutController.current.refresh();}));priorities.getItems().add(item);}menu.getItems().add(priorities);MenuItem delete=new MenuItem("Xóa");delete.setOnAction(x->Ui.safe(()->{if(Ui.confirm("Xóa task?")){a.tasks.delete(t.id());MainLayoutController.current.refresh();}}));menu.getItems().add(delete);menu.show(row,e.getScreenX(),e.getScreenY());});return row;});
        root.setTop(Ui.page("Tất cả công việc",Ui.row(search,project,priority,status,Ui.button("Xóa lọc",()->{search.clear();project.setValue(null);priority.setValue(null);status.setValue(null);}),Ui.button("Xuất CSV",()->{var f=Ui.files("CSV UTF-8","*.csv").showSaveDialog(a.stage);if(f!=null){try{CsvUtil.write(f.toPath(),new ArrayList<>(sorted));Ui.toast("Đã xuất danh sách đang lọc");}catch(java.io.IOException e){throw new com.taskflow.exception.AppException("Không thể ghi CSV",e);}}}),Ui.button("+ Task",()->MainLayoutController.current.newTask(null)))));root.setCenter(table);BorderPane.setMargin(table,new javafx.geometry.Insets(0,26,26,26));
    }
    private <T> void add(TableView<Task> table,String label,Function<Task,T> getter){TableColumn<Task,T> col=new TableColumn<>(label);col.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(getter.apply(v.getValue())));col.setPrefWidth(label.equals("Tiêu đề")?260:130);if(label.equals("Ưu tiên"))col.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(T value,boolean empty){super.updateItem(value,empty);setText(null);if(empty||value==null){setGraphic(null);return;}Label badge=new Label(value.toString());badge.getStyleClass().addAll("badge",value.toString().toLowerCase());setGraphic(badge);}});table.getColumns().add(col);}
}
