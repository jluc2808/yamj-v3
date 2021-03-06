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

import java.util.Collection;
import java.util.Date;
import org.yamj.core.database.model.type.OverrideFlag;

public interface IScannable {

    String getSourceDbId(String sourceDb);

    boolean setSourceDbId(String sourceDb, String id);

    void removeSourceDbIds(Collection<String> sourceDbs);

    boolean removeSourceDbId(String sourceDb);
    
    String getOverrideSource(OverrideFlag overrideFlag);

    void setOverrideFlag(OverrideFlag overrideFlag, String sourceDb);
    
    Date getLastScanned();
    
    int getRetries();

    boolean isAllScansSkipped();
    
    boolean isSkippedScan(String sourceDb);
}