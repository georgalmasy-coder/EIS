package com.bepa.eis.server.dataprovider.cache;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

import java.net.URL;

public final class EhcacheProvider {

    private static final CacheManager CACHE_MANAGER = buildAndInit();

    private EhcacheProvider() {
    }

    public static CacheManager getCacheManager() {
        return CACHE_MANAGER;
    }

    private static CacheManager buildAndInit() {
        URL xml = EhcacheProvider.class.getResource("/ehcache.xml");
        if (xml == null) {
            throw new IllegalStateException("Missing ehcache.xml on classpath (expected at /ehcache.xml)");
        }

        XmlConfiguration xmlConfig = new XmlConfiguration(xml);
        CacheManager cacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);
        cacheManager.init();

        // Nice shutdown (good in app-server / dev)
        Runtime.getRuntime().addShutdownHook(new Thread(cacheManager::close, "ehcache-shutdown"));

        return cacheManager;
    }
}
