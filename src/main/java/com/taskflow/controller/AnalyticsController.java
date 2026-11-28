package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.ui.*;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
public final class AnalyticsController {
    @FXML private BorderPane root;
    @FXML public void initialize() {
        MainApp a=MainApp.app;var tasks=a.tasks.list();var m=a.analytics.metrics(tasks);
        ComboBox<String> period=new ComboBox<>();period.getItems().addAll("8 tuần gần nhất","6 tháng gần nhất");period.getSelectionModel().selectFirst();
        BarChart<String,Number> chart=new BarChart<>(new CategoryAxis(),new NumberAxis());chart.setTitle("Công việc hoàn thành");chart.setLegendVisible(false);
        Runnable refresh=()->{
            XYChart.Series<String,Number> series=new XYChart.Series<>();boolean weeks=period.getSelectionModel().getSelectedIndex()==0;int count=weeks?8:6;
            for(int i=count-1;i>=0;i--){LocalDate start=weeks?LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(i):LocalDate.now().withDayOfMonth(1).minusMonths(i);LocalDate end=weeks?start.plusWeeks(1):start.plusMonths(1);long n=tasks.stream().filter(t->t.completedAt()!=null&&!t.completedAt().toLocalDate().isBefore(start)&&t.completedAt().toLocalDate().isBefore(end)).count();series.getData().add(new XYChart.Data<>(start.toString(),n));}chart.getData().setAll(java.util.List.of(series));
        };period.setOnAction(e->refresh.run());refresh.run();
        FlowPane pies=new FlowPane(12,12);
        for(var p:a.projects.list()){var own=tasks.stream().filter(t->t.projectId()==p.id()).toList();long done=own.stream().filter(t->t.completedAt()!=null).count();double rate=own.isEmpty()?0:100.0*done/own.size();PieChart pie=new PieChart();pie.setPrefSize(390,320);pie.setTitle(p.name()+" · "+String.format("%.1f%%",rate));pie.getData().addAll(new PieChart.Data("Hoàn thành",done),new PieChart.Data("Còn lại",own.size()-done));pies.getChildren().add(pie);}
        root.setCenter(Ui.scroll(Ui.page("Thống kê & Báo cáo",Ui.muted(String.format("Thời gian hoàn thành trung bình: %.1f giờ    ·    Hoàn thành đúng hạn: %.1f%%",m.averageHours(),m.onTimePercent())),period,chart,pies,Ui.muted("Đúng hạn chỉ tính task đã hoàn thành có deadline. Mỗi biểu đồ tròn hiển thị tỉ lệ hoàn thành của một project."))));
    }
}
