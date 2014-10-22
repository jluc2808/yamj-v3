/*
 *      Copyright (c) 2004-2014 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.model;

import java.io.Serializable;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.*;
import org.yamj.core.database.model.type.JobType;
import org.yamj.core.database.model.type.ParticipationType;

@Entity
@Table(name = "participation",
    uniqueConstraints = @UniqueConstraint(name = "UIX_PARTICIPATION_NATURALID", columnNames = {"person_id", "sourcedb", "sourcedb_id"})
)
public class FilmParticipation extends AbstractAuditable implements Serializable {

    private static final long serialVersionUID = 8182882526775933702L;

    @NaturalId(mutable = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @ForeignKey(name = "FK_PARTICIPATION_PERSON")
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @NaturalId(mutable = true)
    @Column(name = "sourcedb", nullable = false, length = 40)
    private String sourceDb;

    @NaturalId(mutable = true)
    @Column(name = "sourcedb_id", nullable = false, length = 40)
    private String sourceDbId;

    @Type(type = "jobType")
    @Column(name = "job", nullable = false, length = 30)
    private JobType jobType;

    @Column(name = "role", length = 255)
    private String role;

    @Type(type = "participationType")
    @Column(name = "participation_type", nullable = false, length = 15)
    private ParticipationType participationType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "title_original", length = 255)
    private String titleOriginal;
    
    @Lob
    @Column(name = "description", length = 50000)
    private String description;

    @Column(name = "year", nullable = false)
    private int year = -1;

    @Column(name = "year_end", nullable = false)
    private int yearEnd = -1;

    @Column(name = "release_date")
    private String releaseDate;

    // GETTER and SETTER
    
    public String getSourceDb() {
        return sourceDb;
    }

    public void setSourceDb(String sourceDb) {
        this.sourceDb = sourceDb;
    }

    public String getSourceDbId() {
        return sourceDbId;
    }

    public void setSourceDbId(String sourceDbId) {
        this.sourceDbId = sourceDbId;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
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

    public ParticipationType getParticipationType() {
        return participationType;
    }

    public void setParticipationType(ParticipationType participationType) {
        this.participationType = participationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleOriginal() {
        return titleOriginal;
    }

    public void setTitleOriginal(String titleOriginal) {
        this.titleOriginal = titleOriginal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
    
    public int getYearEnd() {
        return yearEnd;
    }

    public void setYearEnd(int yearEnd) {
        this.yearEnd = yearEnd;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    // TRANSIENT METHODS
    
    public void merge(FilmParticipation newFilmo) {
        this.setJobType(newFilmo.getJobType());
        this.setRole(newFilmo.getRole());
        this.setParticipationType(newFilmo.getParticipationType());
        this.setYear(newFilmo.getYear());
        this.setYearEnd(newFilmo.getYearEnd());
        this.setTitle(newFilmo.getTitle());
        this.setTitleOriginal(newFilmo.getTitleOriginal());
        this.setDescription(newFilmo.getDescription());
        this.setReleaseDate(newFilmo.getReleaseDate());
    }
    
    // EQUALITY CHECKS

    @Override
    public int hashCode() {
        final int prime = 7;
        int result = 1;
        result = prime * result + (this.sourceDb == null ? 0 : this.sourceDb.hashCode());
        result = prime * result + (this.sourceDbId == null ? 0 : this.sourceDbId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!(other instanceof FilmParticipation)) {
            return false;
        }
        FilmParticipation castOther = (FilmParticipation) other;
        if (!StringUtils.equalsIgnoreCase(this.sourceDb, castOther.sourceDb)) {
            return false;
        }
        if (!StringUtils.equalsIgnoreCase(this.sourceDbId, castOther.sourceDbId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Participation [ID=");
        sb.append(getId());
        sb.append(", sourceDb=");
        sb.append(getSourceDb());
        sb.append(", sourceDbId=");
        sb.append(getSourceDbId());
        sb.append(", title=");
        sb.append(getTitle());
        sb.append(", year=");
        sb.append(getYear());
        sb.append("]");
        return sb.toString();
    }
}