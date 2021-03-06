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
package org.yamj.core.service.artwork;

import static org.yamj.core.ServiceConstants.STORAGE_ERROR;

import java.util.*;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.config.ConfigServiceWrapper;
import org.yamj.core.database.model.*;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.ArtworkLocatorService;
import org.yamj.core.database.service.ArtworkStorageService;
import org.yamj.core.scheduling.IQueueProcessService;
import org.yamj.core.service.attachment.Attachment;
import org.yamj.core.service.attachment.AttachmentScannerService;
import org.yamj.core.service.file.FileTools;
import org.yamj.plugin.api.artwork.*;
import org.yamj.plugin.api.model.*;
import org.yamj.plugin.api.model.mock.*;
import org.yamj.plugin.api.model.type.ArtworkType;
import org.yamj.plugin.api.model.type.ImageType;

@Service("artworkScannerService")
public class ArtworkScannerService implements IQueueProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtworkScannerService.class);
    private static final String USE_SCANNER_FOR = "Use {} scanner for {}";
    private static final String SCANNER_NOT_REG_MOVIE = "Movie artwork scanner {} not registered";
    private static final String SCANNER_NOT_REG_SERIES= "Series artwork scanner {} not registered";
    private static final String SCANNER_NOT_REG_BOXEDSET = "BoxedSet artwork scanner {} not registered";
    private static final String SCANNER_NOT_REG_PERSON = "Person artwork scanner {} not registered";
    
    private final HashMap<String, MovieArtworkScanner> registeredMovieArtworkScanner = new HashMap<>();
    private final HashMap<String, SeriesArtworkScanner> registeredSeriesArtworkScanner = new HashMap<>();
    private final HashMap<String, PersonArtworkScanner> registeredPersonArtworkScanner = new HashMap<>();
    private final HashMap<String, BoxedSetArtworkScanner> registeredBoxedSetArtworkScanner = new HashMap<>();
    
    @Autowired
    private ArtworkLocatorService artworkLocatorService;
    @Autowired
    private ArtworkStorageService artworkStorageService;
    @Autowired
    private AttachmentScannerService attachmentScannerService;
    @Autowired
    private ConfigServiceWrapper configServiceWrapper;
    
    public void registerArtworkScanner(ArtworkScanner artworkScanner) {
        final String scannerName = artworkScanner.getScannerName().toLowerCase();
        if (artworkScanner instanceof MovieArtworkScanner) {
            LOG.trace("Registered movie artwork scanner: {}", scannerName);
            registeredMovieArtworkScanner.put(scannerName, (MovieArtworkScanner)artworkScanner);
        }
        if (artworkScanner instanceof SeriesArtworkScanner) {
            LOG.trace("Registered series artwork scanner: {}", scannerName);
            registeredSeriesArtworkScanner.put(scannerName, (SeriesArtworkScanner)artworkScanner);
        }
        if (artworkScanner instanceof BoxedSetArtworkScanner) {
            LOG.trace("Registered boxed set artwork scanner: {}", scannerName);
            registeredBoxedSetArtworkScanner.put(scannerName, (BoxedSetArtworkScanner)artworkScanner);
        }
        if (artworkScanner instanceof PersonArtworkScanner) {
            LOG.trace("Registered person artwork scanner: {}", scannerName);
            registeredPersonArtworkScanner.put(scannerName, (PersonArtworkScanner)artworkScanner);
        }
    }

    @Override
    public void processQueueElement(QueueDTO queueElement) {
        // get unique required artwork
        Artwork artwork = artworkStorageService.getRequiredArtwork(queueElement.getId());

        // holds the located artwork
        List<ArtworkLocated> locatedArtworks = new ArrayList<>();

        if (ArtworkType.POSTER == artwork.getArtworkType()) {
            // poster only for movie, season, series and boxed sets
            this.scanPosterLocal(artwork, locatedArtworks);
            this.scanPosterAttached(artwork, locatedArtworks);
            this.scanPosterOnline(artwork, locatedArtworks);
        } else if (ArtworkType.FANART == artwork.getArtworkType()) {
            // fanart only for movie, season, series and boxed sets
            this.scanFanartLocal(artwork, locatedArtworks);
            this.scanFanartAttached(artwork, locatedArtworks);
            this.scanFanartOnline(artwork, locatedArtworks);
        } else if (ArtworkType.BANNER == artwork.getArtworkType()) {
            // banner only for season, series and boxed sets
            this.scanBannerLocal(artwork, locatedArtworks);
            this.scanBannerAttached(artwork, locatedArtworks);
            this.scanBannerOnline(artwork, locatedArtworks);
        } else if (ArtworkType.VIDEOIMAGE == artwork.getArtworkType()) {
            // video image only for episodes
            this.scanVideoImageLocal(artwork, locatedArtworks);
            this.scanVideoImageAttached(artwork, locatedArtworks);
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
            LOG.warn(STORAGE_ERROR, error);
        }
    }

    @Override
    public void processErrorOccurred(QueueDTO queueElement, Exception error) {
        LOG.error("Failed scan for artwork "+queueElement.getId(), error);

        artworkStorageService.errorArtwork(queueElement.getId());
    }
    
    private void scanPosterLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local poster scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for poster: {}", artwork);
        List<StageFile> posters = Collections.emptyList();

        if (artwork.getVideoData() != null) {
            // scan movie poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getVideoData());
        } else if (artwork.getSeason() != null) {
            // scan season poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getSeason());
        } else if (artwork.getSeries() != null) {
            // scan series poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getSeries());
        } else if (artwork.getBoxedSet() != null) {
            // scan boxed set poster
            posters = this.artworkLocatorService.getMatchingArtwork(ArtworkType.POSTER, artwork.getBoxedSet());
        }

        createLocatedArtworksLocal(artwork, posters, locatedArtworks);
    }

    private void scanPosterAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached poster scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for poster: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }

    private void scanPosterOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online poster scan disabled: {}", artwork);
            return;
        }
        
        LOG.debug("Scan online for poster: {}", artwork);
        List<ArtworkDTO> posters = null;
        int maxResults = 0;
        
        if (artwork.getBoxedSet() != null) {
            // CASE: boxed set poster
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.poster.boxset.maxResults", 5);
            IBoxedSet iBoxedSet = buildBoxedSet(artwork.getBoxedSet());

            for (String prio : determinePriorities("yamj3.artwork.scanner.poster.boxset.priorities", registeredBoxedSetArtworkScanner.keySet())) {
                BoxedSetArtworkScanner scanner = registeredBoxedSetArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_BOXEDSET, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    posters = scanner.getPosters(iBoxedSet);
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                }
            }
        } else if (artwork.getVideoData() != null && artwork.getVideoData().isMovie()) {
            // CASE: movie poster
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.poster.movie.maxResults", 5);
            IMovie iMovie = buildMovie(artwork.getVideoData());

            for (String prio : determinePriorities("yamj3.artwork.scanner.poster.movie.priorities", registeredMovieArtworkScanner.keySet())) {
                MovieArtworkScanner scanner = registeredMovieArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_MOVIE, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    posters = scanner.getPosters(iMovie);
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                }
            }
        } else if (artwork.getSeason() != null || artwork.getSeries() != null) {
            // CASE: TV show poster scan
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.poster.tvshow.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.poster.tvshow.priorities", registeredSeriesArtworkScanner.keySet())) {
                SeriesArtworkScanner scanner = registeredSeriesArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_SERIES, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    if (artwork.getSeries() != null) {
                        posters = scanner.getPosters(buildSeries(artwork.getSeries()));
                    } else {
                        posters = scanner.getPosters(buildSeason(artwork.getSeason()));
                    }
                    if (CollectionUtils.isNotEmpty(posters)) {
                        break;
                    }
                }
            }
        }

        if (posters == null || posters.isEmpty()) {
            LOG.info("No poster found for: {}", artwork);
            return;
        }

        if (maxResults > 0 && posters.size() > maxResults) {
            LOG.info("Limited posters to {}, actually retrieved {} for {}", maxResults, posters.size(), artwork);
            posters = posters.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, posters, locatedArtworks);
    }

    private void scanFanartLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local fanart scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for fanart: {}", artwork);
        List<StageFile> fanarts  = Collections.emptyList();

        if (artwork.getVideoData() != null) {
            // scan movie fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getVideoData());
        } else if (artwork.getSeason() != null) {
            // scan season fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getSeason());
        } else if (artwork.getSeries() != null) {
            // scan series fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getSeries());
        } else if (artwork.getBoxedSet() != null) {
            // scan boxed set fanart
            fanarts = this.artworkLocatorService.getMatchingArtwork(ArtworkType.FANART, artwork.getBoxedSet());
        }

        createLocatedArtworksLocal(artwork, fanarts, locatedArtworks);
    }

    private void scanFanartAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached fanart scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for fanart: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }

    private void scanFanartOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online fanart scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for fanart: {}", artwork);
        List<ArtworkDTO> fanarts = null;
        int maxResults = 0;
        
        if (artwork.getBoxedSet() != null) {
            // CASE: boxed set fanart
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.fanart.boxset.maxResults", 5);
            IBoxedSet iBoxedSet = buildBoxedSet(artwork.getBoxedSet());
            
            for (String prio : determinePriorities("yamj3.artwork.scanner.fanart.boxset.priorities", registeredBoxedSetArtworkScanner.keySet())) {
                BoxedSetArtworkScanner scanner = registeredBoxedSetArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_BOXEDSET, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    fanarts = scanner.getFanarts(iBoxedSet);
                    if (CollectionUtils.isNotEmpty(fanarts)) {
                        break;
                    }
                }
            }
        } else if (artwork.getVideoData() != null && artwork.getVideoData().isMovie()) {
            // CASE: movie fanart
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.fanart.movie.maxResults", 5);
            IMovie iMovie = buildMovie(artwork.getVideoData());

            for (String prio : determinePriorities("yamj3.artwork.scanner.fanart.movie.priorities", registeredMovieArtworkScanner.keySet())) {
                MovieArtworkScanner scanner = registeredMovieArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_MOVIE, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    fanarts = scanner.getFanarts(iMovie);
                    if (CollectionUtils.isNotEmpty(fanarts)) {
                        break;
                    }
                }
            }
        } else if (artwork.getSeason() != null || artwork.getSeries() != null) {
            // CASE: TV show fanart
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.fanart.tvshow.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.fanart.tvshow.priorities", registeredSeriesArtworkScanner.keySet())) {
                SeriesArtworkScanner scanner = registeredSeriesArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_SERIES, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    if (artwork.getSeries() != null) {
                        fanarts = scanner.getFanarts(buildSeries(artwork.getSeries()));
                    } else {
                        fanarts = scanner.getFanarts(buildSeason(artwork.getSeason()));
                    }
                    if (CollectionUtils.isNotEmpty(fanarts)) {
                        break;
                    }
                }
            }
        }


        if (fanarts == null || fanarts.isEmpty()) {
            LOG.info("No fanart found for: {}", artwork);
            return;
        }

        if (maxResults > 0 && fanarts.size() > maxResults) {
            LOG.info("Limited fanarts to {}, actually retrieved {} for {}", maxResults, fanarts.size(), artwork);
            fanarts = fanarts.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, fanarts, locatedArtworks);
    }

    private void scanBannerLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local banner scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for TV show banner: {}", artwork);
        List<StageFile> banners = Collections.emptyList();

        if (artwork.getSeason() != null) {
            // scan season banner
            banners = this.artworkLocatorService.getMatchingArtwork(ArtworkType.BANNER, artwork.getSeason());
        } else if (artwork.getSeries() != null) {
            // scan series banner
            banners = this.artworkLocatorService.getMatchingArtwork(ArtworkType.BANNER, artwork.getSeries());
        } else if (artwork.getBoxedSet() != null) {
            // scan boxed set banner
            banners = this.artworkLocatorService.getMatchingArtwork(ArtworkType.BANNER, artwork.getBoxedSet());
        }

        createLocatedArtworksLocal(artwork, banners, locatedArtworks);
    }

    private void scanBannerAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached banner scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for banner: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }

    private void scanBannerOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online banner scan disabled: {}", artwork);
            return;
        }

        LOG.debug("Scan online for banner: {}", artwork);
        List<ArtworkDTO> banners = null;
        int maxResults = 0;
        
        if (artwork.getBoxedSet() != null) {
            // CASE: boxed set banner
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.banner.boxset.maxResults", 5);
            IBoxedSet iBoxedSet = buildBoxedSet(artwork.getBoxedSet());

            for (String prio : determinePriorities("yamj3.artwork.scanner.banner.boxset.priorities", registeredBoxedSetArtworkScanner.keySet())) {
                BoxedSetArtworkScanner scanner = registeredBoxedSetArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_BOXEDSET, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    banners = scanner.getBanners(iBoxedSet);
                    if (CollectionUtils.isNotEmpty(banners)) {
                        break;
                    }
                }
            }
        } else if (artwork.getSeason() != null || artwork.getSeries() != null) {
            // CASE: TV show banner
            maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.banner.tvshow.maxResults", 5);

            for (String prio : determinePriorities("yamj3.artwork.scanner.banner.tvshow.priorities", registeredSeriesArtworkScanner.keySet())) {
                SeriesArtworkScanner scanner = registeredSeriesArtworkScanner.get(prio);
                if (scanner == null) {
                    LOG.warn(SCANNER_NOT_REG_SERIES, prio);
                } else {
                    LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                    if (artwork.getSeries() != null) {
                        banners = scanner.getBanners(buildSeries(artwork.getSeries()));
                    } else {
                        banners = scanner.getBanners(buildSeason(artwork.getSeason()));
                    }
                    if (CollectionUtils.isNotEmpty(banners)) {
                        break;
                    }
                }
            }
        }

        if (banners == null || banners.isEmpty()) {
            LOG.info("No banner found for: {}", artwork);
            return;
        }

        if (maxResults > 0 && banners.size() > maxResults) {
            LOG.info("Limited banner to {}, actually retrieved {} for {}", maxResults, banners.size(), artwork);
            banners = banners.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, banners, locatedArtworks);
    }

    private void scanVideoImageLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local episode image scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for TV show episode image: {}", artwork);

        List<StageFile> videoimages = Collections.emptyList();
        
        if (artwork.getVideoData() != null) {
            int episodePart;
            // due the fact that multiple episodes can be contained in one media file we first have to determine how many
            // episodes are contained in one media file
            List<Long> videoDataIds = this.artworkLocatorService.getVideoEpisodes(artwork.getVideoData());
            if (videoDataIds.size() > 1) {
                // determine the episode part using the ordered index of video datas
                int index = videoDataIds.indexOf(artwork.getVideoData().getId());
                if (index < 0) {
                    episodePart = 0;
                } else {
                    // increase 1 cause index starts at 0
                    episodePart = index+1;
                }
            } else {
                // we just have 1 part
                episodePart = 0;
            }
            
            // scan for matching video images
            videoimages = this.artworkLocatorService.getMatchingEpisodeImages(artwork.getVideoData(), episodePart);
        }
        
        createLocatedArtworksLocal(artwork, videoimages, locatedArtworks);
    }

    private void scanVideoImageAttached(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isAttachedArtworkScanEnabled(artwork)) {
            LOG.trace("Attached episode image scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan attachments for TV show episode image: {}", artwork);
        List<Attachment> attachments = attachmentScannerService.scan(artwork);
        createLocatedArtworksAttached(artwork, attachments, locatedArtworks);
    }
    
    private void scanVideoImageOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online episode image scan disabled: {}", artwork);
            return;
        }

        VideoData videoData = artwork.getVideoData();
        if (videoData == null || videoData.isMovie()) {
            LOG.warn("No associated episode found for artwork: {}", artwork);
            return;
        }

        LOG.debug("Scan online for TV show episode image: {}", artwork);
        List<ArtworkDTO> videoimages = Collections.emptyList();
        
        for (String prio : determinePriorities("yamj3.artwork.scanner.videoimage.priorities", registeredSeriesArtworkScanner.keySet())) {
            SeriesArtworkScanner scanner = registeredSeriesArtworkScanner.get(prio);
            if (scanner == null) {
                LOG.warn(SCANNER_NOT_REG_SERIES, prio);
            } else {
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), artwork);
                videoimages = scanner.getVideoImages(buildEpisode(videoData));
                if (CollectionUtils.isNotEmpty(videoimages)) {
                    break;
                }
            }
        }

        if (CollectionUtils.isEmpty(videoimages)) {
            LOG.info("No TV show episode image found for: {}", artwork);
            return;
        }

        int maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.videoimage.maxResults", 2);
        if (maxResults > 0 && videoimages.size() > maxResults) {
            LOG.info("Limited TV show episode images to {}, actually retrieved {} for {}", maxResults, videoimages.size(), artwork);
            videoimages = videoimages.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, videoimages, locatedArtworks);
    }

    private void scanPhotoLocal(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isLocalArtworkScanEnabled(artwork)) {
            LOG.trace("Local photo scan disabled: {}", artwork);
            return;
        }

        LOG.trace("Scan local for photo: {}", artwork);
        List<StageFile> photos = Collections.emptyList();

        if (artwork.getPerson() != null) {
            // scan person photo
            photos = this.artworkLocatorService.getPhotos(artwork.getPerson());
        }

        createLocatedArtworksLocal(artwork, photos, locatedArtworks);
    }

    private void scanPhotoOnline(Artwork artwork, List<ArtworkLocated> locatedArtworks) {
        if (!configServiceWrapper.isOnlineArtworkScanEnabled(artwork, locatedArtworks)) {
            LOG.trace("Online photo scan disabled: {}", artwork);
            return;
        }

        Person person = artwork.getPerson();
        if (person == null) {
            LOG.warn("No associated person found for artwork: {}", artwork);
            return;
        }

        LOG.debug("Scan online for photo: {}", artwork);
        List<ArtworkDTO> photos = null;
        IPerson iPerson = buildPerson(person);
        
        for (String prio : determinePriorities("yamj3.artwork.scanner.photo.priorities", registeredPersonArtworkScanner.keySet())) {
            PersonArtworkScanner scanner = registeredPersonArtworkScanner.get(prio);
            if (scanner == null) {
                LOG.warn(SCANNER_NOT_REG_PERSON, prio);
            } else {
                LOG.debug(USE_SCANNER_FOR, scanner.getScannerName(), person);
                photos = scanner.getPhotos(iPerson);
                if (CollectionUtils.isNotEmpty(photos)) {
                    break;
                }
            }
        }

        if (photos == null || photos.isEmpty()) {
            LOG.info("No photos found for: {}", artwork);
            return;
        }

        int maxResults = this.configServiceWrapper.getIntProperty("yamj3.artwork.scanner.photo.maxResults", 1);
        if (maxResults > 0 && photos.size() > maxResults) {
            LOG.info("Limited photos to {}, actually retrieved {} for {}", maxResults, photos.size(), artwork);
            photos = photos.subList(0, maxResults);
        }

        createLocatedArtworksOnline(artwork, photos, locatedArtworks);
    }

    private static void createLocatedArtworksOnline(Artwork artwork, List<ArtworkDTO> dtos, List<ArtworkLocated> locatedArtworks) {
        for (ArtworkDTO dto : dtos) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource(dto.getSource());
            located.setUrl(dto.getUrl());
            located.setHashCode(dto.getHashCode());
            located.setImageType(dto.getImageType());
            located.setLanguageCode(dto.getLanguageCode());
            located.setRating(dto.getRating());
            located.setStatus(StatusType.NEW);
            located.setPriority(10);
            
            if (!locatedArtworks.contains(located)) {
                locatedArtworks.add(located);
            }
        }
    }

    private static void createLocatedArtworksLocal(Artwork artwork, List<StageFile> stageFiles, List<ArtworkLocated> locatedArtworks) {
        for (StageFile stageFile : stageFiles) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource("file");
            located.setPriority(1);
            located.setStageFile(stageFile);
            located.setHashCode(stageFile.getHashCode());
            located.setImageType(ImageType.fromString(stageFile.getExtension()));
            
            if (FileTools.isFileReadable(stageFile)) {
                located.setStatus(StatusType.NEW);
            } else {
                located.setStatus(StatusType.INVALID);
            }
            
            if (!locatedArtworks.contains(located)) {
                locatedArtworks.add(located);
            }
        }
    }

    private static void createLocatedArtworksAttached(Artwork artwork, List<Attachment> attachments, List<ArtworkLocated> locatedArtworks) {
        for (Attachment attachment : attachments) {
            ArtworkLocated located = new ArtworkLocated();
            located.setArtwork(artwork);
            located.setSource("attachment#"+attachment.getAttachmentId());
            located.setPriority(8);
            located.setStageFile(attachment.getStageFile());
            located.setHashCode(attachment.getStageFile().getHashCode(attachment.getAttachmentId()));
            located.setImageType(attachment.getImageType());
            
            if (FileTools.isFileReadable(attachment.getStageFile())) {
                located.setStatus(StatusType.NEW);
            } else {
                located.setStatus(StatusType.INVALID);
            }
            
            if (!locatedArtworks.contains(located)) {
                locatedArtworks.add(located);
            }
        }
    }

    private Set<String> determinePriorities(String configkey, Set<String> possibleScanners) {
        final String configValue = this.configServiceWrapper.getProperty(configkey, "");
        Set<String> result = ArtworkStorageTools.determinePriorities(configValue, possibleScanners);
        LOG.trace("{} --> {}", configkey, result);
        return result;
    }

    private static IMovie buildMovie(VideoData videoData) {
        MovieMock mock = new MovieMock(videoData.getIdMap());
        mock.setTitle(videoData.getTitle());
        mock.setOriginalTitle(videoData.getTitleOriginal());
        mock.setYear(videoData.getPublicationYear());
        return mock;
    }

    private static ISeries buildSeries(Series series) {
        SeriesMock mock = new SeriesMock(series.getIdMap());
        mock.setTitle(series.getTitle());
        mock.setOriginalTitle(series.getTitleOriginal());
        mock.setStartYear(series.getStartYear());
        mock.setEndYear(series.getEndYear());
        return mock;
    }

    private static ISeason buildSeason(Season season) {
        SeasonMock mock = new SeasonMock(season.getSeason(), season.getIdMap());
        mock.setTitle(season.getTitle());
        mock.setOriginalTitle(season.getTitleOriginal());
        mock.setYear(season.getPublicationYear());
        mock.setSeries(buildSeries(season.getSeries()));
        return mock;
    }

    private static IEpisode buildEpisode(VideoData videoData) {
        EpisodeMock mock = new EpisodeMock(videoData.getEpisode(), videoData.getIdMap());
        mock.setTitle(videoData.getTitle());
        mock.setOriginalTitle(videoData.getTitleOriginal());
        mock.setSeason(buildSeason(videoData.getSeason()));
        return mock;
    }
    
    private static IPerson buildPerson(Person person) {
        PersonMock mock = new PersonMock(person.getIdMap());
        mock.setName(person.getName());
        return mock;
    } 
    
    private static IBoxedSet buildBoxedSet(BoxedSet boxedSet) {
        BoxedSetMock mock = new BoxedSetMock(boxedSet.getIdMap());
        mock.setName(boxedSet.getName());
        return mock;
    } 
}
