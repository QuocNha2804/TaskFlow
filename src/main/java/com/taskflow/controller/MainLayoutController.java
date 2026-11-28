package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.model.*;
import com.taskflow.ui.*;
import com.taskflow.ui.component.*;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;

public final class MainLayoutController {
    public static MainLayoutController current;
    @FXML private BorderPane root;
    private VBox sidebar; private StackPane content;
    public Long selectedProject; private String screen="dashboard";
    @FXML public void initialize() {
        current=this; MainApp a=MainApp.app;
        sidebar=new VBox(10); sidebar.setPrefWidth(245); sidebar.setPadding(new javafx.geometry.Insets(20)); sidebar.getStyleClass().add("sidebar");
        content=new StackPane(); ScrollPane sideScroll=Ui.scroll(sidebar); sideScroll.setFitToHeight(true); root.setLeft(sideScroll); root.setCenter(content);
        HBox top=Ui.row(Ui.muted("WORKSPACE  /  TASKFLOW"),new Region(),Ui.button("⌕  Tìm kiếm  Ctrl+K",()->QuickSearchDialog.open()),Ui.button("◐",()->{ a.settings.dark(!a.settings.dark()); ThemeManager.apply(root.getScene(),a.settings.dark()); }),new AvatarBadge(a.auth.user()),new Label(a.auth.user().fullName()),Ui.button("Đăng xuất",a::logout));
        HBox.setHgrow(top.getChildren().get(1),Priority.ALWAYS); top.setPadding(new javafx.geometry.Insets(14,24,14,24)); top.getStyleClass().add("topbar"); root.setTop(top);
        root.addEventFilter(KeyEvent.KEY_PRESSED,e->{
            if(e.isControlDown()&&e.getCode()==KeyCode.K) { QuickSearchDialog.open(); e.consume(); }
            if(e.isControlDown()&&e.getCode()==KeyCode.N) { newTask(null); e.consume(); }
        }); refreshSidebar(); navigate("dashboard");
    }
    public void refreshSidebar() {
        MainApp a=MainApp.app; sidebar.getChildren().setAll(Ui.title("◈ TaskFlow"),Ui.muted("Không gian làm việc"));
        String[][] nav={{"dashboard","◫  Tổng quan"},{"task_table","☷  Tất cả công việc"},{"calendar","▦  Lịch công việc"},{"analytics","▥  Thống kê"},{"settings","⚙  Cài đặt & Hồ sơ"}};
        for(String[] n:nav) { Button b=Ui.button(n[1],()->navigate(n[0])); b.setMaxWidth(Double.MAX_VALUE); sidebar.getChildren().add(b); }
        sidebar.getChildren().addAll(new Separator(),Ui.row(Ui.muted("PROJECT"),Ui.button("+",()->ProjectDialogController.open(null))));
        var tasks=a.tasks.list();
        for(Project p:a.projects.list()) {
            var own=tasks.stream().filter(t->t.projectId()==p.id()).toList();
            double rate=own.isEmpty()?0:(double)own.stream().filter(t->t.completedAt()!=null).count()/own.size();
            Button b=Ui.button((p.archived()?"▣ ":"● ")+p.name()+"  "+(int)(rate*100)+"%",()->{selectedProject=p.id(); navigate("board");}); b.setMaxWidth(Double.MAX_VALUE);
            b.setStyle("-fx-border-width: 0 0 0 3; -fx-border-color: transparent transparent transparent "+p.color()+";");
            ProgressBar bar=new ProgressBar(rate); bar.setMaxWidth(Double.MAX_VALUE); bar.setPrefHeight(4); sidebar.getChildren().add(new VBox(5,b,bar));
        }
        if(a.projects.list().isEmpty()) sidebar.getChildren().add(Ui.muted("Tạo project đầu tiên bằng nút +"));
    }
    public void navigate(String view) { screen=view; Parent page=ViewManager.load("view/"+view); content.getChildren().setAll(page); ViewManager.animate(page); }
    public void refresh() { refreshSidebar(); navigate(screen); }
    public void newTask(java.time.LocalDate date) { Ui.safe(()->{
        var available=MainApp.app.projects.list().stream().filter(p->!p.archived()).toList();
        if(available.isEmpty()) { Ui.toast("Hãy tạo một project đang hoạt động trước."); return; }
        Project p=available.stream().filter(x->selectedProject!=null&&x.id()==selectedProject).findFirst().orElse(available.getFirst());
        TaskDialogController.open(null,p.id(),null,date);
    }); }
}
