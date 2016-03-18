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
package org.yamj.core.scheduling;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.yamj.core.config.ConfigService;
import org.yamj.core.database.model.dto.QueueDTO;
import org.yamj.core.database.service.TrailerStorageService;
import org.yamj.core.service.trailer.TrailerProcessorService;

@Component
public class TrailerProcessScheduler extends AbstractQueueScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(TrailerProcessScheduler.class);
    private static final ReentrantLock PROCESS_LOCK = new ReentrantLock();

    @Autowired
    private ConfigService configService;
    @Autowired
    private TrailerStorageService trailerStorageService;
    @Autowired
    private TrailerProcessorService trailerProcessorService;
    
    private boolean messageDisabled = false; // Have we already printed the disabled message
    private final AtomicBoolean watchProcess = new AtomicBoolean(false);

    @Scheduled(initialDelay = 5000, fixedDelay = 300000)
    public void trigger() {
        LOG.trace("Trigger trailer processing");
        watchProcess.set(true);
    }

    @Scheduled(initialDelay = 6000, fixedDelay = 1000)
    public void run() {
        if (watchProcess.get() && PROCESS_LOCK.tryLock()) {
            try {
                processTrailer();
            } finally {
                PROCESS_LOCK.unlock();
            }
        }
    }
    
    private void processTrailer() {
        int maxThreads = configService.getIntProperty("yamj3.scheduler.trailerprocess.maxThreads", 0);
        if (maxThreads <= 0) {
            if (!messageDisabled) {
                messageDisabled = Boolean.TRUE;
                LOG.info("Trailer processing is disabled");
            }
            watchProcess.set(false);
            return;
        }
        
        if (messageDisabled) {
            LOG.info("Trailer processing is enabled");
            messageDisabled = Boolean.FALSE;
        }

        int maxResults = Math.max(1,configService.getIntProperty("yamj3.scheduler.trailerprocess.maxResults", 50));
        List<QueueDTO> queueElements = trailerStorageService.getTrailerQueueForProcessing(maxResults);
        if (CollectionUtils.isEmpty(queueElements)) {
            LOG.trace("No trailer found to process");
            watchProcess.set(false);
        } else {
            LOG.info("Found {} trailer objects to process; process with {} threads", queueElements.size(), maxThreads);
            this.threadedProcessing(queueElements, maxThreads, trailerProcessorService);
            LOG.debug("Finished trailer processing");
        }
    }
}
