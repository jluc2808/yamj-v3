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
package org.yamj.core.service.artwork;

import org.apache.commons.lang3.StringUtils;

public class ArtworkTools {

    /**
     * Get the hash code based on the URL.
     * 
     * @param url
     * @return the hash code
     */
    public static String getSimpleHashCode(String url) {
        String hashCode = null;
        try {
            int index = StringUtils.lastIndexOf(url, "/");
            if (index > -1) {
                String tmp = url.substring(index+1);
                index = tmp.indexOf(".");
                if (index > -1) {
                    hashCode = tmp.substring(0, index);
                }
            }
        } catch (Exception e) {
            // ignore any error
        }
        return hashCode;
    }

}