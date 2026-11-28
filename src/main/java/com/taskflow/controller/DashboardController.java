package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.model.Task;
import com.taskflow.ui.*;
import com.taskflow.util.DateUtil;
import com.taskflow.dao.JdbcDao;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

public final class DashboardController {
    @FXML private BorderPane root;
    @FXML public void initialize() {
        MainApp a=MainApp.app;var tasks=a.tasks.list();var m=a.analytics.metrics(tasks);
        HBox kpis=Ui.row(kpi("Task hôm nay",m.today()),kpi("Task quá hạn",m.overdue()),kpi("Hoàn thành tuần này",m.completedWeek()),kpi("Project đang chạy",a.projects.list().stream().filter(p->!p.archived()).count()));
        for(var n:kpis.getChildren()) HBox.setHgrow(n,Priority.ALWAYS);
        PieChart pie=new PieChart();pie.setTitle("Trạng thái công việc");Map<String,Long> counts=new LinkedHashMap<>();for(Task t:tasks) counts.merge(t.columnName(),1L,Long::sum);counts.forEach((name,n)->pie.getData().add(new PieChart.Data(name,n)));
        BarChart<String,Number> bar=new BarChart<>(new CategoryAxis(),new NumberAxis());bar.setTitle("Hoàn thành trong 7 ngày");bar.setLegendVisible(false);XYChart.Series<String,Number> series=new XYChart.Series<>();a.analytics.completions(tasks,7).forEach((d,n)->series.getData().add(new XYChart.Data<>(d.getDayOfMonth()+"/"+d.getMonthValue(),n)));bar.getData().add(series);
        HBox charts=Ui.row(pie,bar);pie.setPrefWidth(420);bar.setPrefWidth(600);HBox.setHgrow(bar,Priority.ALWAYS);
        ListView<Task> due=new ListView<>();due.setPrefHeight(250);due.setPlaceholder(Ui.muted("Không có task sắp đến hạn. Bạn đang làm rất tốt!"));
        due.getItems().setAll(tasks.stream().filter(t->t.completedAt()==null&&t.deadline()!=null).sorted(Comparator.comparing(Task::deadline)).limit(12).toList());
        due.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(Task t,boolean empty){super.updateItem(t,empty);setText(empty||t==null?null:t.title()+" · "+t.projectName()+" · "+DateUtil.format(t.deadline()));setStyle(!empty&&t!=null&&t.overdue()?"-fx-text-fill: #e05b60;":"");}});
        due.setOnMouseClicked(e->{Task t=due.getSelectionModel().getSelectedItem();if(t!=null)TaskDialogController.open(t.id(),t.projectId(),null,null);});
        VBox recent=new VBox(8);for(var log:a.tasks.recent())recent.getChildren().add(Ui.muted(DateUtil.format(JdbcDao.time(log,"created_at"))+" · "+JdbcDao.string(log,"title")+" · "+JdbcDao.string(log,"action")));
        if(recent.getChildren().isEmpty())recent.getChildren().add(Ui.muted("Hoạt động của bạn sẽ xuất hiện ở đây."));
        root.setCenter(Ui.scroll(Ui.page("Tổng quan",Ui.muted("Xin chào, "+a.auth.user().fullName()+" · Hôm nay bạn muốn hoàn thành điều gì?"),kpis,charts,Ui.row(Ui.title("Sắp đến hạn & Quá hạn"),Ui.button("+ Công việc mới",()->MainLayoutController.current.newTask(null))),due,Ui.title("Hoạt động gần đây"),recent)));
    }
    private VBox kpi(String label,long value){VBox box=new VBox(8,Ui.muted(label),Ui.title(Long.toString(value)));box.getStyleClass().add("kpi");box.setMaxWidth(Double.MAX_VALUE);return box;}
}
