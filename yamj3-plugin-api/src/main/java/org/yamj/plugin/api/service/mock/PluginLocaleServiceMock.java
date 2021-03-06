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
package org.yamj.plugin.api.service.mock;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import org.yamj.plugin.api.service.PluginLocaleService;

public class PluginLocaleServiceMock implements PluginLocaleService {

    private final Locale locale;
    
    public PluginLocaleServiceMock() {
        this.locale = Locale.getDefault();
    }

    public PluginLocaleServiceMock(Locale locale) {
        this.locale = locale;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }
    
    @Override
    public String findCountryCode(String country) {
        return country;
    }

    @Override
    public String findLanguageCode(String language) {
        return language;
    }

    @Override
    public Set<String> getCountryNames(String countryCode) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getCertificationCountryCodes(Locale locale) {
        return Collections.emptySet();
    }

    @Override
    public String getDisplayCountry(String inLanguage, String countryCode) {
        return countryCode;
    }

    @Override
    public String getDisplayLanguage(String inLanguage, String languageCode) {
        return languageCode;
    }
}
