/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
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
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yamj.core.api.model.Parameters;
import org.yamj.core.api.model.dto.IndexDTO;
import org.yamj.core.api.options.OptionsIndex;
import org.yamj.core.database.dao.ApiDao;
import org.yamj.core.database.dao.ArtworkDao;
import org.yamj.core.database.dao.CommonDao;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.type.MetaDataType;

@Service("jsonApiStorageService")
public class JsonApiStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(JsonApiStorageService.class);
    @Autowired
    private CommonDao commonDao;
    @Autowired
    private ArtworkDao artworkDao;
    @Autowired
    private ApiDao apiDao;

    @Transactional(readOnly = true)
    public List<IndexDTO> getVideoList(OptionsIndex options) {
        Map<String, String> includes = options.getIncludes();
        Map<String, String> excludes = options.getExcludes();
        StringBuilder sql = new StringBuilder();

        // Add the movie entries
        if (options.getType().equals("ALL") || options.getType().equals("MOVIE")) {
            sql.append("SELECT vd.id");
            sql.append(", '").append(MetaDataType.MOVIE).append("' AS video_type");
            sql.append(", vd.title");
            sql.append(", vd.publication_year");
            sql.append(" FROM videodata vd");
            // Add genre tables for include and exclude
            if (includes.containsKey("genre") || excludes.containsKey("genre")) {
                sql.append(", videodata_genres vg, genre g");
            }

            sql.append(" WHERE vd.episode < 0");
            // Add joins for genres
            if (includes.containsKey("genre") || excludes.containsKey("genre")) {
                sql.append("AND vd.id=vg.data_id");
                sql.append("AND vg.genre_id=g.id");
                sql.append("AND and g.name='");
                if (includes.containsKey("genre")) {
                    sql.append(includes.get("genre"));
                } else {
                    sql.append(excludes.get("genre"));
                }
                sql.append("'");
            }

            if (includes.containsKey("year")) {
                sql.append(" AND vd.publication_year=").append(includes.get("year"));
            }

            if (excludes.containsKey("year")) {
                sql.append(" AND vd.publication_year!=").append(includes.get("year"));
            }
        }

        if (options.getType().equals("ALL")) {
            sql.append(" UNION ");
        }

        // Add the TV entires
        if (options.getType().equals("ALL") || options.getType().equals("TV")) {
            sql.append("SELECT ser.id");
            sql.append(", '").append(MetaDataType.SERIES).append("' AS video_type");
            sql.append(", ser.title");
            sql.append(", ser.start_year");
            sql.append(" FROM series ser ");
            sql.append(" WHERE 1"); // To make it easier to add the optional include and excludes

            if (includes.containsKey("year")) {
                sql.append(" AND ser.start_year=").append(includes.get("year"));
            }

            if (excludes.containsKey("year")) {
                sql.append(" AND ser.start_year!=").append(includes.get("year"));
            }
        }

        if (StringUtils.isNotBlank(options.getSortby())) {
            sql.append(" ORDER BY ");
            sql.append(options.getSortby()).append(" ");
            sql.append(options.getSortdir().toUpperCase());
        }

        LOG.info("INDEX SQL: {}", sql);

        return apiDao.getVideoList(sql.toString(), options);
    }

    @Transactional(readOnly = true)
    public <T> T getEntityById(Class<T> entityClass, Serializable id) {
        return commonDao.getById(entityClass, id);
    }

    //<editor-fold defaultstate="collapsed" desc="VideoData Methods">
    @Transactional(readOnly = true)
    public List<VideoData> getVideoList(Parameters params) {
        return commonDao.getVideoList(params);
    }

    @Transactional(readOnly = true)
    public List<Series> getSeriesList(Parameters params) {
        return commonDao.getSeriesList(params);
    }

    @Transactional(readOnly = true)
    public List<Season> getSeasonList(Parameters params) {
        return commonDao.getSeasonList(params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Genre Methods">
    @Transactional(readOnly = true)
    public Genre getGenre(Serializable id) {
        return commonDao.getById(Genre.class, id);
    }

    @Transactional(readOnly = true)
    public Genre getGenre(String name) {
        return commonDao.getByName(Genre.class, name);
    }

    @Transactional(readOnly = true)
    public List<Genre> getGenres(Parameters params) {
        return commonDao.getList(Genre.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Certification Methods">
    @Transactional(readOnly = true)
    public Certification getCertification(Serializable id) {
        return commonDao.getById(Certification.class, id);
    }

    @Transactional(readOnly = true)
    public Certification getCertification(String name) {
        return commonDao.getByName(Certification.class, name);
    }

    @Transactional(readOnly = true)
    public List<Certification> getCertifications(Parameters params) {
        return commonDao.getList(Certification.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Boxed Set Methods">
    @Transactional(readOnly = true)
    public BoxedSet getBoxedSet(Serializable id) {
        return commonDao.getById(BoxedSet.class, id);
    }

    @Transactional(readOnly = true)
    public BoxedSet getBoxedSet(String name) {
        return commonDao.getByName(BoxedSet.class, name);
    }

    @Transactional(readOnly = true)
    public List<BoxedSet> getBoxedSets(Parameters params) {
        return commonDao.getList(BoxedSet.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Studio Methods">
    @Transactional(readOnly = true)
    public Studio getStudio(Serializable id) {
        return commonDao.getById(Studio.class, id);
    }

    @Transactional(readOnly = true)
    public Studio getStudio(String name) {
        return commonDao.getByName(Studio.class, name);
    }

    @Transactional(readOnly = true)
    public List<Studio> getStudios(Parameters params) {
        return commonDao.getList(Studio.class, params);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Artwork Methods">
    @Transactional(readOnly = true)
    public List<Artwork> getArtworkList(Parameters params) {
        return artworkDao.getArtworkList(params);
    }
    //</editor-fold>
}
