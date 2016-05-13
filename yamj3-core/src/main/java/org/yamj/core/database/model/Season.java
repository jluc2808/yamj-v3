/*
 *      Copyright (c) 2004-2015 YAMJ Members
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

import java.util.*;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.*;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.type.OverrideFlag;

@NamedQueries({
    @NamedQuery(name = Season.QUERY_IDS_RECHECK,
        query = "SELECT sea.id FROM Season sea WHERE sea.status not in ('NEW','UPDATED') AND (sea.lastScanned is null or sea.lastScanned<=:compareDate)"
    ),
    @NamedQuery(name = Season.UPDATE_STATUS_RECHECK,
        query = "UPDATE Season sea SET sea.status='UPDATED' WHERE sea.id in (:idList)"
    ),
    @NamedQuery(name = Season.UPDATE_RESCAN_ALL,
        query = "UPDATE Season SET status='UPDATED' WHERE status not in ('NEW','UPDATED')"
    ),
})

@Entity
@Table(name = "season",
        uniqueConstraints = @UniqueConstraint(name = "UIX_SEASON_NATURALID", columnNames = {"identifier"}),
        indexes = {@Index(name = "IX_SEASON_TITLE", columnList = "title"),
                   @Index(name = "IX_SEASON_STATUS", columnList = "status"),
                   @Index(name = "IX_SEASON_SEASON", columnList = "season"),
                   @Index(name = "IX_SEASON_PUBLICATIONYEAR", columnList = "publication_year")}
)
@SuppressWarnings("unused")
public class Season extends AbstractMetadata {

    private static final long serialVersionUID = 1858640563119637343L;
    public static final String QUERY_IDS_RECHECK = "season.ids.forRecheck";
    public static final String UPDATE_STATUS_RECHECK = "season.updateStatus.forRecheck";
    public static final String UPDATE_RESCAN_ALL = "season.rescanAll";
    
    @Column(name = "season", nullable = false)
    private int season; //NOSONAR

    @Column(name = "publication_year", nullable = false)
    private int publicationYear = -1;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "season_ids",
            joinColumns = @JoinColumn(name = "season_id"), foreignKey = @ForeignKey(name = "FK_SEASON_SOURCEIDS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "sourcedb_id", length = 200, nullable = false)
    private Map<String, String> sourceDbIdMap = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "season_ratings", 
            joinColumns = @JoinColumn(name = "season_id"), foreignKey = @ForeignKey(name = "FK_SEASON_RATINGS"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "sourcedb", length = 40)
    @Column(name = "rating", nullable = false)
    private Map<String, Integer> ratings = new HashMap<>(0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "season_override",
            joinColumns = @JoinColumn(name = "season_id"), foreignKey = @ForeignKey(name = "FK_SEASON_OVERRIDE"))
    @Fetch(FetchMode.JOIN)
    @MapKeyColumn(name = "flag", length = 30)
    @MapKeyType(value = @Type(type = "overrideFlag"))
    @Column(name = "source", length = 30, nullable = false)
    private Map<OverrideFlag, String> overrideFlags = new EnumMap<>(OverrideFlag.class);

    @ManyToOne(fetch = FetchType.LAZY)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "series_id", nullable = false, foreignKey = @ForeignKey(name = "FK_SEASON_SERIES"))
    private Series series;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "season")
    private Set<VideoData> videoDatas = new HashSet<>(0);

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "season")
    private List<Artwork> artworks = new ArrayList<>(0);

    // CONSTRUCTORS

    public Season() {
        super();
    }

    public Season(String identifier) {
        super(identifier);
    }

    // GETTER and SETTER

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    private void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public void setPublicationYear(int publicationYear, String source) {
        if (publicationYear > 0) {
            setPublicationYear(publicationYear);
            setOverrideFlag(OverrideFlag.YEAR, source);
        }
    }

    public void removePublicationYear(String source) {
        if (hasOverrideSource(OverrideFlag.YEAR, source)) {
            setPublicationYear(-1);
            removeOverrideFlag(OverrideFlag.YEAR);
        }
    }

    @Override
    protected Map<String, String> getSourceDbIdMap() {
        return sourceDbIdMap;
    }

    private void setSourceDbIdMap(Map<String, String> sourceDbIdMap) {
        this.sourceDbIdMap = sourceDbIdMap;
    }
    
    private Map<String, Integer> getRatings() {
        return ratings;
    }

    private void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
    }

    public void addRating(String sourceDb, int rating) {
        if (StringUtils.isNotBlank(sourceDb) && (rating >= 0)) {
            getRatings().put(sourceDb, rating);
        }
    }

    public void removeRating(String sourceDb) {
        if (StringUtils.isNotBlank(sourceDb)) {
            getRatings().remove(sourceDb);
        }
    }

    @Override
    protected Map<OverrideFlag, String> getOverrideFlags() {
        return overrideFlags;
    }

    private void setOverrideFlags(Map<OverrideFlag, String> overrideFlags) {
        this.overrideFlags = overrideFlags;
    }

    @Override
    protected String getSkipScanApi() {
        return null;
    }

    @Override
    protected void setSkipScanApi(String skipScanApi) {
        // nothing to do
    }
    
    @Override
    public boolean isAllScansSkipped() {
        return getSeries().isAllScansSkipped();
    }
    
    @Override
    public boolean isSkippedScan(String sourceDb) {
        return getSeries().isSkippedScan(sourceDb);
    }

    public Series getSeries() {
        return series;
    }

    public void setSeries(Series series) {
        this.series = series;
    }

    public Set<VideoData> getVideoDatas() {
        return videoDatas;
    }

    private void setVideoDatas(Set<VideoData> videoDatas) {
        this.videoDatas = videoDatas;
    }

    public List<Artwork> getArtworks() {
        return artworks;
    }

    private void setArtworks(List<Artwork> artworks) {
        this.artworks = artworks;
    }
    
    // TV CHECKS

    public boolean isTvSeasonDone(String sourceDb) {
        if (StringUtils.isBlank(getSourceDbId(sourceDb))) {
            // not done if episode ID not set
            return false;
        }
        return StatusType.DONE.equals(getStatus());
    }
    
    public void setTvSeasonDone() {
        setStatus(StatusType.TEMP_DONE);
    }

    public void setTvSeasonNotFound() {
        if (StatusType.DONE.equals(getStatus())) {
            // reset to temporary done state
            setStatus(StatusType.TEMP_DONE);
        } else if (!StatusType.TEMP_DONE.equals(getStatus())) {
            // do not reset temporary done
            setStatus(StatusType.NOTFOUND);
        }
    }

    // EQUALITY CHECKS
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getIdentifier())
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Season)) {
            return false;
        }
        Season other = (Season) obj;
        // first check the id
        if ((getId() > 0) && (other.getId() > 0)) {
            return getId() == other.getId();
        }
        // check other values
        return new EqualsBuilder()
                .append(getIdentifier(), other.getIdentifier())
                .isEquals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Season [ID=");
        sb.append(getId());
        sb.append(", identifier=");
        sb.append(getIdentifier());
        sb.append(", title=");
        sb.append(getTitle());
        sb.append(", year=");
        sb.append(getPublicationYear());
        sb.append("]");
        return sb.toString();
    }
}
