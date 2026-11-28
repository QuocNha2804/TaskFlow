package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.ui.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
public final class SettingsController {
    @FXML private BorderPane root;
    @FXML public void initialize(){
        MainApp a=MainApp.app;CheckBox dark=new CheckBox("Giao diện tối");dark.setSelected(a.settings.dark());dark.setOnAction(e->{a.settings.dark(dark.isSelected());ThemeManager.apply(root.getScene(),dark.isSelected());});
        CheckBox reminders=new CheckBox("Bật thông báo deadline");reminders.setSelected(a.settings.notifications());reminders.setOnAction(e->a.settings.notifications(reminders.isSelected()));
        ComboBox<Integer> hours=new ComboBox<>();hours.getItems().addAll(1,6,24);hours.setValue(a.settings.reminderHours());hours.setOnAction(e->a.settings.reminderHours(hours.getValue()));
        TextField name=new TextField(a.auth.user().fullName()),avatar=new TextField(a.auth.user().avatarUrl());ColorPicker color=new ColorPicker(javafx.scene.paint.Color.web(a.auth.user().avatarColor()));
        Button picture=Ui.button("Chọn ảnh đại diện",()->{FileChooser chooser=Ui.files("Ảnh","*.png");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPEG","*.jpg","*.jpeg"));var f=chooser.showOpenDialog(a.stage);if(f!=null)avatar.setText(f.toURI().toString());});
        PasswordField oldPassword=new PasswordField(),newPassword=new PasswordField();
        VBox page=Ui.page("Cài đặt & Hồ sơ",Ui.title("Giao diện & Thông báo"),dark,reminders,Ui.field("Nhắc trước deadline (giờ)",hours),Ui.muted("Ứng dụng kiểm tra mỗi 5 phút khi đang mở; mỗi deadline chỉ nhắc một lần. Đổi deadline sẽ cho phép nhắc lại."),new Separator(),Ui.title("Hồ sơ cá nhân"),Ui.field("Tên hiển thị",name),Ui.field("Màu avatar",color),Ui.field("Ảnh đại diện (URI file hoặc URL)",avatar),picture,Ui.button("Lưu hồ sơ",()->{String hex=String.format("#%02x%02x%02x",(int)(color.getValue().getRed()*255),(int)(color.getValue().getGreen()*255),(int)(color.getValue().getBlue()*255));a.auth.profile(name.getText(),hex,avatar.getText());a.showMain();Ui.toast("Đã cập nhật hồ sơ");}),new Separator(),Ui.title("Đổi mật khẩu"),Ui.field("Mật khẩu hiện tại",oldPassword),Ui.field("Mật khẩu mới",newPassword),Ui.button("Đổi mật khẩu",()->{a.auth.password(oldPassword.getText(),newPassword.getText());oldPassword.clear();newPassword.clear();Ui.toast("Đã đổi mật khẩu và thu hồi các phiên ghi nhớ");}),new Separator(),Ui.title("Sao lưu & Khôi phục"),Ui.muted("JSON chứa các project bạn tham gia, thành viên, task, tag, checklist, bình luận và lịch sử. Khôi phục tạo project bản sao do bạn sở hữu; thành viên chỉ được liên kết nếu tài khoản đã tồn tại."),Ui.row(Ui.button("Xuất JSON",()->{var f=Ui.files("TaskFlow backup","*.json").showSaveDialog(a.stage);if(f!=null)try{a.exports.exportJson(f.toPath());Ui.toast("Đã sao lưu JSON");}catch(java.io.IOException e){throw new com.taskflow.exception.AppException("Không thể ghi file",e);}}),Ui.button("Nhập JSON",()->{var f=Ui.files("TaskFlow backup","*.json").showOpenDialog(a.stage);if(f!=null&&Ui.confirm("Khôi phục thành project bản sao từ file này?"))try{int count=a.exports.importJson(f.toPath());MainLayoutController.current.refreshSidebar();Ui.toast("Đã khôi phục "+count+" project");}catch(java.io.IOException e){throw new com.taskflow.exception.AppException("Không thể đọc file",e);}})));
        page.setMaxWidth(1000);root.setCenter(Ui.scroll(page));
    }
}
