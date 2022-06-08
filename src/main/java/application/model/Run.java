package application.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;

@Entity
@Table(name ="run")
public class Run {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long athleteId;
    private double distance;
    private double pace;
    private LocalDate date;
    private long movingTime;
    private double totalPoint;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getPace() {
        return pace;
    }

    public void setPace(double pace) {
        this.pace = pace;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getMovingTime() {
        return movingTime;
    }

    public void setMovingTime(long movingTime) {
        this.movingTime = movingTime;
    }

    public long getAthleteId() {
        return athleteId;
    }

    public void setAthleteId(long athleteId) {
        this.athleteId = athleteId;
    }

    public void setTotalPoint(double totalPoint){
        this.totalPoint = totalPoint;
    }

    public  double getTotalPoint(){
        return totalPoint;
    }
}
