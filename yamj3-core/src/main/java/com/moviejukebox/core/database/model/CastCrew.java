package com.moviejukebox.core.database.model;

import com.moviejukebox.core.database.model.type.JobType;
import com.moviejukebox.core.hibernate.usertypes.EnumStringUserType;
import java.io.Serializable;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Parameter;

@TypeDef(name = "jobType", 
    typeClass = EnumStringUserType.class,
    parameters = {@Parameter(name = "enumClassName", value = "com.moviejukebox.core.database.model.type.JobType")})

@Entity
@Table(name = "cast_crew",
       uniqueConstraints = {@UniqueConstraint(columnNames={"person_id", "data_id", "job"})})
public class CastCrew extends AbstractIdentifiable implements Serializable {

    private static final long serialVersionUID = -3941301942248344131L;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CASTCREW_PERSON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id")
    private Person person;

    @NaturalId
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_CASTCREW_VIDEODATA")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name="data_id", insertable=false, updatable=false, nullable=false)
    private VideoData videoData;

    @Type(type = "jobType")
    @Column(name = "job", nullable = false, length = 30)
    private JobType jobType;

    @Column(name = "role", length = 255)
    private String role;

    // GETTER and SETTER

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public VideoData getVideoData() {
        return videoData;
    }

    public void setVideoData(VideoData videoData) {
        this.videoData = videoData;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
