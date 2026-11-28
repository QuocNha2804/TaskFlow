package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.model.*;
import com.taskflow.ui.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public final class ProjectDialogController {
    private ProjectDialogController() {}
    public static void open(Project p) {
        MainApp a=MainApp.app;
        TextField name=new TextField(p==null?"":p.name()),icon=new TextField(p==null?"folder":p.icon());
        TextArea description=new TextArea(p==null?"":p.description()); description.setPrefRowCount(3);
        ColorPicker color=new ColorPicker(Color.web(p==null?"#6366f1":p.color()));
        DatePicker start=new DatePicker(p==null?null:p.startDate()),end=new DatePicker(p==null?null:p.endDate());
        CheckBox archived=new CheckBox("Lưu trữ project"); archived.setSelected(p!=null&&p.archived());
        VBox form=(VBox)ViewManager.load("component/project_dialog");
        form.getChildren().addAll(Ui.title(p==null?"Tạo project":"Quản lý project"),Ui.field("Tên",name),Ui.field("Mô tả",description),Ui.row(Ui.field("Màu",color),Ui.field("Icon / ký hiệu",icon)),Ui.row(Ui.field("Bắt đầu",start),Ui.field("Kết thúc",end)),archived);
        form.setPrefWidth(610); Stage stage=Ui.modal("Project",Ui.scroll(form));
        if(p!=null) {
            VBox members=new VBox(8); Runnable reload=()->{ members.getChildren().clear(); for(User u:a.projects.members(p.id())) members.getChildren().add(Ui.row(new Label(u+(u.id()==p.ownerId()?" · Owner":" · Member")),Ui.button("Xóa",()->{ if(Ui.confirm("Xóa thành viên "+u.fullName()+"?")) { a.projects.removeMember(p.id(),u.id()); stage.close(); open(a.projects.require(p.id())); } }))); }; reload.run();
            TextField username=new TextField(); username.setPromptText("Tên đăng nhập đã đăng ký");
            form.getChildren().addAll(new Separator(),new Label("Thành viên"),members,Ui.row(username,Ui.button("Thêm",()->{a.projects.addMember(p.id(),username.getText()); reload.run(); username.clear();})));
        }
        Button save=Ui.button("Lưu project",()->{
            String hex=String.format("#%02x%02x%02x",(int)(color.getValue().getRed()*255),(int)(color.getValue().getGreen()*255),(int)(color.getValue().getBlue()*255));
            long id=a.projects.save(p==null?null:p.id(),name.getText(),description.getText(),hex,icon.getText(),start.getValue(),end.getValue(),archived.isSelected());
            MainLayoutController.current.selectedProject=id; stage.close(); MainLayoutController.current.refresh(); Ui.toast("Đã lưu project");
        }); save.getStyleClass().add("accent");
        HBox actions=Ui.row(save,Ui.button("Đóng",stage::close));
        if(p!=null) actions.getChildren().add(Ui.button("Xóa project",()->{if(Ui.confirm("Xóa project cùng toàn bộ task?")) { a.projects.delete(p.id()); stage.close(); MainLayoutController.current.selectedProject=null; MainLayoutController.current.navigate("dashboard"); MainLayoutController.current.refreshSidebar(); }}));
        form.getChildren().add(actions); stage.showAndWait();
    }
}
