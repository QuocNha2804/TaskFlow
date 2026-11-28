package com.taskflow.controller;
import com.taskflow.MainApp;
import com.taskflow.model.Task;
import com.taskflow.ui.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.*;
import java.util.*;
public final class CalendarController {
    @FXML private BorderPane root;
    private YearMonth month=YearMonth.now();private LocalDate selected=LocalDate.now();private final VBox days=new VBox(12);private final GridPane grid=new GridPane();private final Label heading=Ui.title("");
    @FXML public void initialize(){
        root.setTop(Ui.page("Lịch công việc",Ui.row(Ui.button("‹",()->{month=month.minusMonths(1);render();}),heading,Ui.button("›",()->{month=month.plusMonths(1);render();}),Ui.button("Hôm nay",()->{selected=LocalDate.now();month=YearMonth.now();render();}))));
        grid.setHgap(6);grid.setVgap(6);grid.setPadding(new javafx.geometry.Insets(0,20,20,26));
        for(int i=0;i<7;i++){ColumnConstraints c=new ColumnConstraints();c.setPercentWidth(100.0/7);grid.getColumnConstraints().add(c);}
        days.setPrefWidth(300);days.setPadding(new javafx.geometry.Insets(0,20,20,0));root.setCenter(grid);root.setRight(Ui.scroll(days));render();
    }
    private void render(){
        heading.setText("Tháng "+month.getMonthValue()+" / "+month.getYear());grid.getChildren().clear();var tasks=MainApp.app.tasks.list();
        String[] names={"T2","T3","T4","T5","T6","T7","CN"};for(int i=0;i<7;i++)grid.add(new Label(names[i]),i,0);
        LocalDate start=month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue()-1);
        for(int i=0;i<42;i++){
            LocalDate date=start.plusDays(i);var own=tasks.stream().filter(t->t.deadline()!=null&&t.deadline().toLocalDate().equals(date)).toList();
            VBox cell=new VBox(5,new Label(Integer.toString(date.getDayOfMonth())));cell.getStyleClass().add("calendar-day");cell.setMinHeight(80);cell.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
            if(!YearMonth.from(date).equals(month))cell.setOpacity(0.4);if(date.equals(selected))cell.getStyleClass().add("selected-day");
            for(Task t:own.stream().limit(2).toList()){Label dot=new Label("● "+t.title());dot.setMaxWidth(Double.MAX_VALUE);dot.getStyleClass().add(t.overdue()?"overdue":t.priority().name().toLowerCase());cell.getChildren().add(dot);}
            if(own.size()>2)cell.getChildren().add(Ui.muted("+ "+(own.size()-2)+" task"));
            cell.setOnMouseClicked(e->{selected=date;render();});grid.add(cell,i%7,i/7+1);GridPane.setVgrow(cell,Priority.ALWAYS);
        }
        days.getChildren().setAll(Ui.title(selected.toString()),Ui.button("+ Thêm task ngày này",()->MainLayoutController.current.newTask(selected)));
        for(Task t:tasks)if(t.deadline()!=null&&t.deadline().toLocalDate().equals(selected))days.getChildren().add(TaskCardController.card(t));
        if(days.getChildren().size()==2)days.getChildren().add(Ui.muted("Ngày này chưa có công việc."));
    }
}
