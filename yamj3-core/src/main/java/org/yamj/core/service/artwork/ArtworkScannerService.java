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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.configuration.ConfigService;
import org.yamj.core.database.model.Artwork;
import org.yamj.core.database.model.ArtworkLocated;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.StageFile;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.model.type.ArtworkType;
import org.yamj.core.database.service.ArtworkLocatorService;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.service.artwork.fanart.IMovieFanartScanner;
import org.yamj.core.service.artwork.fanart.ITvShowFanartScanner;
import org.yamj.core.service.artwork.photo.IPhotoScanner;
import org.yamj.core.service.artwork.poster.IMoviePosterScanner;
import org.yamj.core.service.artwork.poster.ITvShowPosterScanner;
import org.yamj.core.service.artwork.tv.ITvShowBannerScanner;
import org.yamj.core.service.artwork.tv.ITvShowVideoImageScanner;
import org.yamj.core.service.plugin.TheMovieDbScanner;
import org.yamj.core.service.plugin.TheTVDbScanner;

@Service("artworkScannerService")
public class ArtworkScannerService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    private static final String USE_SCANNER_FOR = "Use {} scanner for {}";
    
    private final HashMap<String, IMoviePosterScanner> registeredMoviePosterScanner = new HashMap<String, IMoviePosterScanner>();
    private final HashMap<String, ITvShowPosterScanner> registeredTvShowPosterScanner = new HashMap<String, ITvShowPosterScanner>();
    private final HashMap<String, IMovieFanartScanner> registeredMovieFanartScanner = new HashMap<String, IMovieFanartScanner>();
    private final HashMap<String, ITvShowFanartScanner> registeredTvShowFanartScanner = new HashMap<String, ITvShowFanartScanner>();
    private final HashMap<String, ITvShowBannerScanner> registeredTvShowBannerScanner = new HashMap<String, ITvShowBannerScanner>();
    private final HashMap<String, ITvShowVideoImageScanner> registeredTvShowVideoImageScanner = new HashMap<String, ITvShowVideoImageScanner>();
    private final HashMap<String, IPhotoScanner> registeredPhotoScanner = new HashMap<String, IPhotoScanner>();
    
    @Autowired
    private ArtworkLocatorService artworkLocatorService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private ConfigService configService;

    public void registerMoviePosterScanner(IMoviePosterScanner posterScanner) {
        LOG.info("Registered movie poster scanner: {}", posterScanner.getScannerName().toLowerCase());
        registeredMoviePosterScanner.put(posterScanner.getScannerName().toLowerCase(), posterScanner);
    }

    public void registerTvShowPosterScanner(ITvShowPosterScanner posterScanner) {
        LOG.info("Registered TV show poster scanner: {}", posterScanner.getScannerName().toLowerCase());
        registeredTvShowPosterScanner.put(posterScanner.getScannerName().toLowerCase(), posterScanner);
    }

    public void registerMovieFanartScanner(IMovieFanartScanner fanartScanner) {
        LOG.info("Registered movie fanart scanner: {}", fanartScanner.getScannerName().toLowerCase());
        registeredMovieFanartScanner.put(fanartScanner.getScannerName().toLowerCase(), fanartScanner);
    }

    public void registerTvShowFanartScanner(ITvShowFanartScanner fanartScanner) {
        LOG.info("Registered TV show fanart scanner: {}", fanartScanner.getScannerName().toLowerCase());
        registeredTvShowFanartScanner.put(fanartScanner.getScannerName().toLowerCase(), fanartScanner);
    }

    public void registerTvShowBannerScanner(ITvShowBannerScanner bannerScanner) {
        LOG.info("Registered TV show banner scanner: {}", bannerScanner.getScannerName().toLowerCase());
        registeredTvShowBannerScanner.put(bannerScanner.getScannerName().toLowerCase(), bannerScanner);
    }

    public void registerTvShowVideoImageScanner(ITvShowVideoImageScanner videoImageScanner) {
        LOG.info("Registered TV show episode image scanner: {}", videoImageScanner.getScannerName().toLowerCase());
        registeredTvShowVideoImageScanner.put(videoImageScanner.getScannerName().toLowerCase(), videoImageScanner);
    }

    public void registerPhotoScanner(IPhotoScanner photoScanner) {
        LOG.info("Registered photo scanner: {}", photoScanner.getScannerName().toLowerCase());
        registeredPhotoScanner.put(photoScanner.getScannerName().toLowerCase(), photoScanner);
    }

    public void scanArtwork(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        // get unique required artwork
        Artwork artwork = artworkStorageService.getRequiredArtwork(queueElement.getId());

        // holds the located artwork
        List<ArtworkLocated> locatedArtworks = new LinkedList<ArtworkLocated>();

        if (ArtworkType.POSTER == artwork.getArtworkType()) {
            this.scanPosterLocal(artwork, locatedArtworks);
            this.scanPosterOnline(artwork, locatedArtworks);
        } else if (ArtworkType.FANART == artwork.getArtworkType()) {
            this.scanFanartLocal(artwork, locatedArtworks);
            this.scanFanartOnline(artwork, locatedArtworks);
        } else if (ArtworkType.BANNER == artwork.getArtworkType() && !artwork.getMetadata().isMovie()) {
            // banner only for season and series
            this.scanBannerLocal(artwork, locatedArtworks);
            this.scanBannerOnline(artwork, locatedArtworks);
        } else if (ArtworkType.VIDEOIMAGE == artwork.getArtworkType() && (artwork.getMetadata().getEpisodeNumber() >= 0)) {
            // video image only for episodes
            this.scanVideoImageLocal(artwork, locatedArtworks);
            this.scanVideoImageOnline(artwork, locatedArtworks);
        } else if (ArtworkType.PHOTO == artwork.getArtworkType()) {
            this.scanPhotoLocal(artwork, locatedArtworks);
            this.scanPhotoOnline(artwork, locatedArtworks);
        } else {
            // Don't throw an exception here, just a debug message for now
            LOG.debug("Artwork scan not implemented for {}", artwork);
        }

        // storage
        try {
            artworkStorageService.updateArtwork(artwork, locatedArtworks);
        } catch (Exception error) {
            // NOTE: status will not be changed
            LOG.error("Failed storing artwork {}-{}", queueElement.getId(), artwork.getArtworkType().toString());
            LOG.warn("Storage error", error);
        }
    }

    private boolean isLocalScanEnabled(Artwork artwork) {
        StringBuffer sb = new StringBuffer();
        sb.append("yamj3.artwork.scan.local.");
        this.addScanArtworkType(artwork, sb);
        
        return this.configService.getBooleanProperty(sb.toString(), Boolean.TRUE);
    }

    private boolean isOnlineScanEnabled(Artwork artwork) {
        StringBuffer sb = new StringBuffer();
        sb.append("yamj3.artwork.scan.online.");
        this.addScanArtworkType(artwork, sb);
        
        return this.configService.getBooleanProperty(sb.toString(), Boolean.TRUE);
    }

    private void addScanArtworkType(Artwork artwork, StringBuffer sb) {
        sb.append(artwork.getArtworkType().name().toLowerCase());
        
        if (ArtworkType.POSTER == artwork.getArtworkType() || ArtworkType.FANART == artwork.getArtworkType()) {
            sb.append(".");
            if (artwork.getMetadata().isMovie()) {
                sb.append("movie");
            } else if (artwork.getMetadata().getSeasonNumber() >= 0) {
                sb.append("tvshow.season");
            } else {
                sb.append("tvshow.series");
            }
        } else if (ArtworkType.BANNER == artwork.getArtworkType()) {
            sb.append(".");
            if (artwork.getMetadata().getSeasonNumber() >= 0) {
                sb.append("tvshow.season");
            } else {
                sb.append("tvshow.series");
            }
        }
    }

    private void scanPosterLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isLocalScanEnabled(artwork)) {
            LOG.trace("Local poster scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for poster: {}", artwork);

        List<StageFile> posters = null;

        if (artwork.getMetadata().isMovie()) {
            posters = this.artworkLocatorService.getMoviePosters(artwork.getVideoData());
        }
        // TODO series/season poster scanning

        createLocatedArtworksLocal(artwork, posters, locatedArtworks);
    }

    private void scanPosterOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isOnlineScanEnabled(artwork)) {
            LOG.trace("Online poster scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for poster: {}", artwork);

        List<ArtworkDetailDTO> posters = null;

        if (artwork.getMetadata().isMovie()) {
            // CASE: movie poster scan
            for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.poster.movie.priorities", TheMovieDbScanner.SCANNER_ID)) {
                IMoviePosterScanner scanner = registeredMoviePosterScanner.get(prio);
                if (scanner != null) {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    posters = scanner.getPosters(artwork.getMetadata());
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                } else {
                    LOG.warn("Desired movie poster scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(posters)) {
                LOG.info("No movie poster found for: {}", artwork);
                return;
            }

            int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.poster.movie.maxResults", 5);
            if (maxResults > 0 && posters.size() > maxResults) {
                LOG.info("Limited movie posters to {}, actually retrieved {} for {}", maxResults, posters.size(), artwork);
                posters = posters.subList(0, maxResults);
            }
        } else {
            // CASE: TV show poster scan
            for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.poster.tvshow.priorities", TheTVDbScanner.SCANNER_ID)) {
                ITvShowPosterScanner scanner = registeredTvShowPosterScanner.get(prio);
                if (scanner != null) {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    posters = scanner.getPosters(artwork.getMetadata());
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                } else {
                    LOG.warn("Desired TV show poster scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(posters)) {
                LOG.info("No TV show poster found for: {}", artwork);
                return;
            }

            int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.poster.tvshow.maxResults", 5);
            if (maxResults > 0 && posters.size() > maxResults) {
                LOG.info("Limited TV show posters to {}, actually retrieved {} for {}", maxResults, posters.size(), artwork);
                posters = posters.subList(0, maxResults);
            }
        }

        createLocatedArtworksOnline(artwork, posters, locatedArtworks);
    }

    private void scanFanartLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isLocalScanEnabled(artwork)) {
            LOG.trace("Local fanart scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for fanart: {}", artwork);

        List<StageFile> fanarts = null;

        if (artwork.getMetadata().isMovie()) {
            fanarts = this.artworkLocatorService.getMovieFanarts(artwork.getVideoData());
        }
        // TODO series/season poster scanning

        createLocatedArtworksLocal(artwork, fanarts, locatedArtworks);
    }

    private void scanFanartOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isOnlineScanEnabled(artwork)) {
            LOG.trace("Online fanart scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for fanart: {}", artwork);

        List<ArtworkDetailDTO> fanarts = null;

        if (artwork.getMetadata().isMovie()) {
            // CASE: movie fanart
            for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.fanart.movie.priorities", TheMovieDbScanner.SCANNER_ID)) {
                IMovieFanartScanner scanner = registeredMovieFanartScanner.get(prio);
                if (scanner != null) {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    fanarts = scanner.getFanarts(artwork.getMetadata());
                    if (CollectionUtils.isNotEmpty(fanarts)) {
                        break;
                    }
                } else {
                    LOG.warn("Desired movie fanart scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(fanarts)) {
                LOG.info("No movie fanart found for: {}", artwork);
                return;
            }

            int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.fanart.movie.maxResults", 5);
            if (maxResults > 0 && fanarts.size() > maxResults) {
                LOG.info("Limited movie fanarts to {}, actually retrieved {} for {}", maxResults, fanarts.size(), artwork);
                fanarts = fanarts.subList(0, maxResults);
            }
        } else {
            // CASE: TV show poster scan
            for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.fanart.tvshow.priorities", TheTVDbScanner.SCANNER_ID)) {
                ITvShowFanartScanner scanner = registeredTvShowFanartScanner.get(prio);
                if (scanner != null) {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    fanarts = scanner.getFanarts(artwork.getMetadata());
                } else {
                    LOG.warn("Desired TV show fanart scanner {} not registerd", prio);
                }
            }

            if (CollectionUtils.isEmpty(fanarts)) {
                LOG.info("No TV show fanarts found for: {}", artwork);
                return;
            }

            int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.fanart.tvshow.maxResults", 5);
            if (maxResults > 0 && fanarts.size() > maxResults) {
                LOG.info("Limited TV show fanart to {}, actually retrieved {} for {}", maxResults, fanarts.size(), artwork);
                fanarts = fanarts.subList(0, maxResults);
            }
        }

        createLocatedArtworksOnline(artwork, fanarts, locatedArtworks);
    }

    private void scanBannerLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isLocalScanEnabled(artwork)) {
            LOG.trace("Local banner scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for TV show banner: {}", artwork);

        // TODO local scan
    }

    private void scanBannerOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isOnlineScanEnabled(artwork)) {
            LOG.trace("Online banner scan disabled: {}", artwork);
            return;
        }
        
        LOG.debug("Scan online for TV show banner: {}", artwork);

        List<ArtworkDetailDTO> banners = null;

        for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.banner.tvshow.priorities", TheTVDbScanner.SCANNER_ID)) {
            ITvShowBannerScanner scanner = registeredTvShowBannerScanner.get(prio);
            if (scanner != null) {
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                banners = scanner.getBanners(artwork.getMetadata());
                if (CollectionUtils.isNotEmpty(banners)) {
                    break;
                }
            } else {
                LOG.warn("Desired TV show banner scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(banners) || banners == null) {
            LOG.info("No TV show banner found for: {}", artwork);
            return;
        }

        int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.banner.tvshow.maxResults", 5);
        if (maxResults > 0 && banners.size() > maxResults) {
            LOG.info("Limited TV show banners to {}, actually retrieved {} for {}", maxResults, banners.size(), artwork);
            banners = banners.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, banners, locatedArtworks);
    }

    private void scanVideoImageLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isLocalScanEnabled(artwork)) {
            LOG.trace("Local episode image scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for TV show episode image: {}", artwork);

        // TODO local scan
    }

    private void scanVideoImageOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isOnlineScanEnabled(artwork)) {
            LOG.trace("Online episode image scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for TV show episode image: {}", artwork);

        List<ArtworkDetailDTO> videoimages = null;

        for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.videoimage.priorities", TheTVDbScanner.SCANNER_ID)) {
            ITvShowVideoImageScanner scanner = registeredTvShowVideoImageScanner.get(prio);
            if (scanner != null) {
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                videoimages = scanner.getVideoImages(artwork.getMetadata());
                if (CollectionUtils.isNotEmpty(videoimages)) {
                    break;
                }
            } else {
                LOG.warn("Desired TV show episode image scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(videoimages) || videoimages == null) {
            LOG.info("No TV show episode image found for: {}", artwork);
            return;
        }

        int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.videoimage.maxResults", 2);
        if (maxResults > 0 && videoimages.size() > maxResults) {
            LOG.info("Limited Video Images to {}, actually retrieved {} for {}", maxResults, videoimages.size(), artwork);
            videoimages = videoimages.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, videoimages, locatedArtworks);
    }

    private void scanPhotoLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isLocalScanEnabled(artwork)) {
            LOG.trace("Local photo scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for photo: {}", artwork);

        // TODO local scan
    }

    private void scanPhotoOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!this.isOnlineScanEnabled(artwork)) {
            LOG.trace("Online photo scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for photo: {}", artwork);

        List<ArtworkDetailDTO> photos = null;

        for (String prio : this.configService.getPropertyAsList("yamj3.artwork.scanner.photo.priorities", TheMovieDbScanner.SCANNER_ID)) {
            IPhotoScanner scanner = registeredPhotoScanner.get(prio);
            if (scanner != null) {
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                Person person = artwork.getPerson();
                if (person == null) {
                    LOG.warn("No associated person found for artwork: {}", artwork);
                } else {
                    String id = person.getPersonId(prio);
                    LOG.info("Scanning for person ID: {}-{}", prio, id);
                    if (StringUtils.isNumeric(id)) {
                        photos = scanner.getPhotos(Integer.parseInt(id));
                    } else {
                        // Id looks to be invalid, so look it up
                        id = scanner.getPersonId(person);
                        // Could check if the ID is null and then use the IMDB id if available
                        photos = scanner.getPhotos(id);
                    }
                    if (CollectionUtils.isNotEmpty(photos)) {
                        break;
                    }
                }
            } else {
                LOG.warn("Desired photo scanner {} not registerd", prio);
            }
        }

        if (CollectionUtils.isEmpty(photos)) {
            LOG.info("No photos found for: {}", artwork);
            return;
        }

        int maxResults = this.configService.getIntProperty("yamj3.artwork.scanner.photo.maxResults", 1);
        if (maxResults > 0 && photos.size() > maxResults) {
            LOG.info("Limited photos to {}, actually retrieved {} for {}", maxResults, photos.size(), artwork);
            photos = photos.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, photos, locatedArtworks);
    }

    private void createLocatedArtworksOnline(Artwork artwork, List<ArtworkDetailDTO> dtos, List<ArtworkLocated> locatedArtworks) {
        if (CollectionUtils.isEmpty(dtos)) {
            return;
        }

        for (ArtworkDetailDTO dto : dtos) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(dto.getSource());
            located.setUrl(dto.getUrl());
            located.setHashCode(dto.getHashCode());
            located.setLanguage(dto.getLanguage());
            located.setRating(dto.getRating());
            located.setStatus(StatusType.NEW);
            located.setPriority(10);
            locatedArtworks.add(located);
        }
    }

    private void createLocatedArtworksLocal(Artwork artwork, List<StageFile> stageFiles, List<ArtworkLocated> locatedArtworks) {
        if (CollectionUtils.isEmpty(stageFiles)) {
            return;
        }
        
        for (StageFile stageFile : stageFiles) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setStageFile(stageFile);
            located.setStatus(StatusType.NEW);
            located.setPriority(1);
            locatedArtworks.add(located);
        }
    }

    public void processingError(QueueDTO queueElement) {
        if (queueElement == null) {
            // nothing to
            return;
        }

        artworkStorageService.errorArtwork(queueElement.getId());
    }
}
