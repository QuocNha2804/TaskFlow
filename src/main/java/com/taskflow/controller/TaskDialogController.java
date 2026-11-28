package com.taskflow.controller;

import com.taskflow.MainApp;
import com.taskflow.model.*;
import com.taskflow.model.enums.Priority;
import com.taskflow.dao.JdbcDao;
import com.taskflow.ui.*;
import com.taskflow.util.DateUtil;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import java.time.*;

public final class TaskDialogController {
    @FXML private VBox root;
    private Long id; private long pid; private Stage stage;
    private TextField title,hour,tags; private TextArea description;
    private ComboBox<BoardColumn> column; private ComboBox<User> assignee; private ComboBox<Priority> priority;
    private DatePicker deadline; private VBox checklist,comments,activity; private ProgressBar progress;
    public static void open(Long id,long pid,Long column,LocalDate date) {
        Ui.safe(()->{
            FXMLLoader loader=new FXMLLoader(TaskDialogController.class.getResource("/fxml/component/task_dialog.fxml"));
            VBox pane;
            try { pane=loader.load(); } catch(java.io.IOException e) { throw new com.taskflow.exception.AppException("Không mở được chi tiết task",e); }
            TaskDialogController c=loader.getController();
            c.stage=Ui.modal("Chi tiết công việc",Ui.scroll(pane)); c.id=id; c.pid=pid; c.build(column,date);
            c.stage.getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.S,KeyCombination.CONTROL_DOWN),()->Ui.safe(c::save));
            c.stage.setWidth(820); c.stage.setHeight(860); c.stage.showAndWait();
        });
    }
    private void build(Long target,LocalDate date) {
        MainApp a=MainApp.app; Task t=id==null?null:a.tasks.get(id);
        title=new TextField(t==null?"":t.title()); description=new TextArea(t==null?"":t.description()); description.setPrefRowCount(3);
        priority=new ComboBox<>(); priority.getItems().setAll(Priority.values()); priority.setValue(t==null?Priority.MEDIUM:t.priority());
        column=new ComboBox<>(); column.getItems().setAll(a.projects.columns(pid));
        column.setValue(column.getItems().stream().filter(c->t!=null?c.id()==t.columnId():target!=null&&c.id()==target).findFirst().orElse(column.getItems().getFirst()));
        assignee=new ComboBox<>(); assignee.getItems().add(null); assignee.getItems().addAll(a.projects.members(pid)); assignee.setPromptText("Chưa giao");
        if(t!=null&&t.assigneeId()!=null) assignee.setValue(assignee.getItems().stream().filter(u->u!=null&&u.id()==t.assigneeId()).findFirst().orElse(null));
        deadline=new DatePicker(t==null?date:t.deadline()==null?null:t.deadline().toLocalDate());
        hour=new TextField(t==null||t.deadline()==null?"23:59":t.deadline().toLocalTime().toString()); hour.setPrefWidth(90);
        tags=new TextField(t==null?"":t.tags()); tags.setPromptText("backend, design, urgent");
        VBox tagChoices=new VBox(4); java.util.Set<String> names=new java.util.TreeSet<>();
        for(Task item:a.tasks.project(pid)) for(String s:item.tags().split(",")) if(!s.isBlank()) names.add(s.strip());
        FlowPane pills=new FlowPane(8,6);
        for(String name:names) { CheckBox check=new CheckBox(name); check.setSelected(java.util.Arrays.stream(tags.getText().split(",")).anyMatch(s->s.strip().equals(name))); check.setOnAction(e->{ java.util.Set<String> selected=new java.util.LinkedHashSet<>(); for(String s:tags.getText().split(",")) if(!s.isBlank()) selected.add(s.strip()); if(check.isSelected()) selected.add(name); else selected.remove(name); tags.setText(String.join(", ",selected)); }); pills.getChildren().add(check); }
        tagChoices.getChildren().addAll(tags,pills);
        root.getChildren().setAll(Ui.title(t==null?"Công việc mới":t.title()),Ui.muted(a.projects.require(pid).name()),Ui.field("Tiêu đề *",title),Ui.field("Mô tả",description),Ui.row(Ui.field("Trạng thái",column),Ui.field("Ưu tiên",priority)),Ui.field("Người được giao",assignee),Ui.row(Ui.field("Deadline (xóa ngày để bỏ hạn)",deadline),Ui.field("Giờ HH:mm",hour)),Ui.field("Tags (chọn hoặc nhập, phân cách dấu phẩy)",tagChoices));
        if(t!=null) {
            root.getChildren().add(Ui.muted("Tạo: "+DateUtil.format(t.createdAt())+" · Cập nhật: "+DateUtil.format(t.updatedAt())+"\nHoàn thành: "+DateUtil.format(t.completedAt())));
            checklist=new VBox(8); comments=new VBox(10); activity=new VBox(6); progress=new ProgressBar(); progress.setMaxWidth(Double.MAX_VALUE);
            TextField sub=new TextField(); sub.setPromptText("Thêm checklist…");
            TextArea comment=new TextArea(); comment.setPromptText("Viết bình luận…"); comment.setPrefRowCount(2);
            root.getChildren().addAll(new Separator(),new Label("CHECKLIST"),progress,checklist,Ui.row(sub,Ui.button("+ Thêm",()->{a.tasks.subtask(id,null,sub.getText(),false);sub.clear();reload();})),new Separator(),new Label("BÌNH LUẬN"),comments,comment,Ui.button("Gửi bình luận",()->{a.tasks.comment(id,comment.getText());comment.clear();reload();}),new Separator(),new Label("LỊCH SỬ HOẠT ĐỘNG"),activity);
            reload();
        } else root.getChildren().add(Ui.muted("Lưu task trước để thêm checklist, bình luận và xem lịch sử."));
        Button save=Ui.button("Lưu · Ctrl+S",this::save); save.getStyleClass().add("accent");
        HBox actions=Ui.row(save,Ui.button("Đóng",stage::close));
        if(id!=null) actions.getChildren().add(Ui.button("Xóa task",()->{ if(Ui.confirm("Xóa task và toàn bộ checklist, bình luận?")) { a.tasks.delete(id);stage.close();MainLayoutController.current.refresh();Ui.toast("Đã xóa task"); } }));
        root.getChildren().add(actions);
        stage.setOnHidden(e->MainLayoutController.current.refresh());
    }
    private void save() {
        LocalDateTime value;
        try { value=DateUtil.deadline(deadline.getValue(),hour.getText()); } catch(java.time.format.DateTimeParseException e) { throw new com.taskflow.exception.ValidationException("Giờ cần đúng HH:mm, ví dụ 17:30."); }
        MainApp.app.tasks.save(id,pid,column.getValue().id(),title.getText(),description.getText(),priority.getValue(),value,assignee.getValue()==null?null:assignee.getValue().id(),tags.getText());
        stage.close(); Ui.toast("Đã lưu công việc");
    }
    private void reload() {
        MainApp a=MainApp.app;
        checklist.getChildren().clear(); var subs=a.tasks.subtasks(id); long done=subs.stream().filter(s->JdbcDao.bool(s,"is_completed")).count(); progress.setProgress(subs.isEmpty()?0:(double)done/subs.size());
        for(var sub:subs) {
            long key=JdbcDao.number(sub,"id"); CheckBox check=new CheckBox(JdbcDao.string(sub,"title")); check.setSelected(JdbcDao.bool(sub,"is_completed"));
            check.setOnAction(e->Ui.safe(()->{a.tasks.subtask(id,key,check.getText(),check.isSelected());reload();}));
            checklist.getChildren().add(Ui.row(check,Ui.button("Sửa",()->{String text=Ui.input("Sửa checklist",check.getText());if(text!=null){a.tasks.subtask(id,key,text,check.isSelected());reload();}}),Ui.button("×",()->{if(Ui.confirm("Xóa mục checklist?")){a.tasks.deleteSubtask(id,key);reload();}})));
        }
        comments.getChildren().clear();
        for(var c:a.tasks.comments(id)) {
            VBox box=new VBox(5,Ui.muted(JdbcDao.string(c,"full_name")+" · "+DateUtil.format(JdbcDao.time(c,"created_at"))),new Label(JdbcDao.string(c,"content")));
            if(JdbcDao.number(c,"user_id")==a.auth.user().id()||a.projects.require(pid).ownerId()==a.auth.user().id()) box.getChildren().add(Ui.button("Xóa bình luận",()->{if(Ui.confirm("Xóa bình luận?")){a.tasks.deleteComment(id,JdbcDao.number(c,"id"));reload();}}));
            comments.getChildren().add(box);
        }
        activity.getChildren().clear(); for(var log:a.tasks.activity(id)) activity.getChildren().add(Ui.muted(DateUtil.format(JdbcDao.time(log,"created_at"))+" · "+JdbcDao.string(log,"full_name")+" · "+JdbcDao.string(log,"action")));
    }
}
