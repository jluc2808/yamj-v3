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
package org.yamj.core.api.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.BasicType;
import org.springframework.util.CollectionUtils;

/**
 * Builds a SQL statement and holds the scalars for the statement
 *
 * @author Stuart
 */
public final class SqlScalars {

    private StringBuilder sql;
    private Map<String, BasicType> scalars;
    private Map<String, Collection> parameterList;
    private Map<String, Object> parameters;
    private SQLQuery query = null;

    public SqlScalars() {
        this.sql = new StringBuilder();
        this.scalars = new HashMap<String, BasicType>();
        this.parameterList = new HashMap<String, Collection>();
        this.parameters = new HashMap<String, Object>();
    }

    public SqlScalars(StringBuilder sql) {
        this.sql = sql;
        this.scalars = new HashMap<String, BasicType>();
        this.parameterList = new HashMap<String, Collection>();
        this.parameters = new HashMap<String, Object>();
    }

    public SqlScalars(String sql) {
        setSql(sql);
        this.scalars = new HashMap<String, BasicType>();
    }

    public SqlScalars(StringBuilder sql, Map<String, BasicType> scalars) {
        this.sql = sql;
        this.scalars = scalars;
    }

    /**
     * Set the SQL
     *
     * @param sql
     */
    public void setSql(StringBuilder sql) {
        this.sql = sql;
    }

    /**
     * Set the SQL using a string
     *
     * @param sql
     */
    public void setSql(String sql) {
        this.sql = new StringBuilder(sql);
    }

    /**
     * Append a line to the SQL.
     *
     * Will add a space at the end of the line
     *
     * @param line
     */
    public void addToSql(String line) {
        sql.append(line);
        if (!line.endsWith(" ")) {
            sql.append(" ");
        }
    }

    /**
     * Get the SQL as a string
     *
     * @return
     */
    public String getSql() {
        return sql.toString();
    }

    /**
     * Get the SQL as a query using the session
     *
     * @param session
     * @return
     */
    public SQLQuery createSqlQuery(Session session) {
        if (this.query == null) {
            this.query = session.createSQLQuery(sql.toString());
            // Add parameters
            if (!CollectionUtils.isEmpty(parameters)) {
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }

            if (!CollectionUtils.isEmpty(parameterList)) {
                for (Map.Entry<String, Collection> entry : parameterList.entrySet()) {
                    query.setParameterList(entry.getKey(), entry.getValue());
                }
            }

        }
        return this.query;
    }

    public void addParameterList(String param, Collection values) {
        this.parameterList.put(param, values);
    }

    public void addParameter(String key, Object value) {
        this.parameters.put(key, value);
    }

    /**
     * Clear the SQL
     */
    public void clearSql() {
        sql = new StringBuilder();
    }

    /**
     * Get the scalars for the query
     *
     * @return
     */
    public Map<String, BasicType> getScalars() {
        return scalars;
    }

    /**
     * Set the scalars for the query
     *
     * @param scalars
     */
    public void setScalars(Map<String, BasicType> scalars) {
        this.scalars = scalars;
    }

    /**
     * Add a scalar with a default type
     *
     * @param scalar
     */
    public void addScalar(String scalar) {
        this.scalars.put(scalar, null);
    }

    /**
     * Add a scalart with type
     *
     * @param scalar
     * @param type
     */
    public void addScalar(String scalar, BasicType type) {
        this.scalars.put(scalar, type);
    }

    /**
     * Add the scalars to the query
     *
     * @param query
     */
    public void populateScalars(SQLQuery query) {
        if (!CollectionUtils.isEmpty(scalars)) {
            for (Map.Entry<String, BasicType> entry : scalars.entrySet()) {
                if (entry.getValue() == null) {
                    // Use the default scalar for that entry
                    query.addScalar(entry.getKey());
                } else {
                    // Use the passed scalar type
                    query.addScalar(entry.getKey(), entry.getValue());
                }
            }
        }
    }
}
