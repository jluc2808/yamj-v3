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
package org.yamj.core.tools;

import java.util.Date;
import org.joda.time.DateTime;

public final class WatchedDTO {

    private Boolean watched = null;
    private Date watchedDate = null;
    
    private boolean watchedVideo = false;
    private Date watchedVideoDate;
    private boolean watchedMediaFile = true;
    private Date watchedMediaFileDate;
    private boolean watchedMediaApi = true;
    private Date watchedMediaApiDate;
  
    public void watchedVideo(boolean watched, Date watchedDate) {
        if (watchedDate == null) {
            return;
        }

        if (this.watchedVideoDate == null) {
            this.watchedVideo = watched;
            this.watchedVideoDate = watchedDate;
        } else if (watchedDate.after(this.watchedVideoDate)) {
            this.watchedVideo = watched;
            this.watchedVideoDate = watchedDate;
        }
    }

    public void watchedMediaFile(boolean watched, Date watchedDate) {
        if (watchedDate == null) {
            return;
        }

        if (this.watchedMediaFileDate == null) {
            this.watchedMediaFile = watched;
            this.watchedMediaFileDate = watchedDate;
        } else {
            this.watchedMediaFile = this.watchedMediaFile && watched;
            if (watchedDate.after(this.watchedMediaFileDate)) {            
                this.watchedMediaFileDate = watchedDate;
            }
        }
    }

    public void watchedMediaApi(boolean watched, Date watchedDate) {
        if (watchedDate == null) {
            return;
        }

        if (this.watchedMediaApiDate == null) {
            this.watchedMediaApi = watched;
            this.watchedMediaApiDate = watchedDate;
        } else {
            this.watchedMediaApi = this.watchedMediaApi && watched;
            if (watchedDate.after(this.watchedMediaApiDate)) {            
                this.watchedMediaApiDate = watchedDate;
            }
        }
    }

    public boolean isWatched() {
        this.evaluateWatched();
        return this.watched.booleanValue();
    }

    public Date getWatchedDate() {
        this.evaluateWatched();
        if (this.watchedDate == null) {
            return null;
        }
        return new DateTime().withMillis(this.watchedDate.getTime()).withMillisOfSecond(0).toDate();
    }

    private void evaluateWatched() {
        if (this.watched != null) {
            return;
        }
        
        boolean locWatched = false;
        Date locWatchedDate = null;
        
        if (this.watchedVideoDate != null) {
            locWatched = this.watchedVideo;
            locWatchedDate =  this.watchedVideoDate;
        }
        
        if (this.watchedMediaFileDate != null) {
            if (locWatchedDate == null) {
                locWatched = this.watchedMediaFile;
                locWatchedDate = this.watchedMediaFileDate;
            } else if (locWatchedDate.before(this.watchedMediaFileDate)) {
                locWatched = this.watchedMediaFile;
                locWatchedDate = this.watchedMediaFileDate;
            }
        }

        if (this.watchedMediaApiDate != null) {
            if (locWatchedDate == null) {
                locWatched = this.watchedMediaApi;
                locWatchedDate = this.watchedMediaApiDate;
            } else if (locWatchedDate.before(this.watchedMediaApiDate)) {
                locWatched = this.watchedMediaApi;
                locWatchedDate = this.watchedMediaApiDate;
            }
        }

        this.watched = Boolean.valueOf(locWatched); 
        this.watchedDate = locWatchedDate;
    }
}
