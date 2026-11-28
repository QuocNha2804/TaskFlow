package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.ui.*;
import com.taskflow.util.Validator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
public final class LoginController {
    @FXML private StackPane root;
    @FXML public void initialize() { render(false); }
    private void render(boolean register) {
        MainApp a=MainApp.app;
        TextField username=new TextField(),email=new TextField(),name=new TextField(); PasswordField password=new PasswordField(),repeat=new PasswordField();
        username.setPromptText("Tên đăng nhập"); password.setPromptText("Ít nhất 8 ký tự");
        CheckBox remember=new CheckBox("Ghi nhớ đăng nhập trong 30 ngày");
        Label validation=Ui.muted(""); validation.setStyle("-fx-text-fill: #e05b60;");
        Button submit=new Button(register?"Tạo tài khoản":"Đăng nhập"); submit.getStyleClass().add("accent"); submit.setMaxWidth(Double.MAX_VALUE); submit.setDefaultButton(true);
        Runnable validate=()->{ try {
            Validator.required(username.getText(),"Tên đăng nhập",50);
            if(register) { Validator.email(email.getText()); Validator.required(name.getText(),"Họ tên",100); Validator.password(password.getText()); if(!password.getText().equals(repeat.getText())) throw new IllegalArgumentException("Mật khẩu xác nhận chưa khớp."); }
            else Validator.required(password.getText(),"Mật khẩu",100);
            validation.setText(""); submit.setDisable(false);
        } catch(Exception e) { validation.setText(e.getMessage()); submit.setDisable(true); } };
        for(TextInputControl f:new TextInputControl[]{username,email,name,password,repeat}) f.textProperty().addListener((o,x,y)->validate.run());
        submit.setDisable(true);
        submit.setOnAction(e->Ui.safe(()->{ if(register) { a.auth.register(username.getText(),email.getText(),name.getText(),password.getText()); Ui.toast("Đăng ký thành công. Hãy đăng nhập."); render(false); } else { a.auth.login(username.getText(),password.getText(),remember.isSelected()); a.showMain(); } }));
        VBox form=new VBox(14,Ui.title("TaskFlow"),Ui.muted(register?"Tạo tài khoản · Bắt đầu tổ chức công việc":"Chào mừng trở lại · Tập trung vào điều quan trọng"),Ui.field("Tên đăng nhập",username));
        if(register) form.getChildren().addAll(Ui.field("Email",email),Ui.field("Họ tên",name));
        form.getChildren().add(Ui.field("Mật khẩu",password));
        if(register) form.getChildren().add(Ui.field("Nhập lại mật khẩu",repeat)); else form.getChildren().add(remember);
        form.getChildren().addAll(validation,submit,Ui.button(register?"Đã có tài khoản? Đăng nhập":"Chưa có tài khoản? Đăng ký",()->render(!register)));
        form.setMaxWidth(430); form.setPadding(new javafx.geometry.Insets(32)); form.getStyleClass().add("surface"); root.getChildren().setAll(form);
    }
}
