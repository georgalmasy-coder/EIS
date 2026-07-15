package com.bepa.eis.server.dataprovider.cache;

import com.bepa.eis.server.api.web.application.cache.LookupCache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.xml.XmlConfiguration;

import java.net.URL;

public final class EhcacheProvider {

    private static final CacheManager CACHE_MANAGER = buildAndInit();

    public static final String LOOKUP_CACHE_ALIAS = "LookupCache";

    private EhcacheProvider() {
    }

    public static CacheManager getCacheManager() {
        return CACHE_MANAGER;
    }

    public static <K, V> void clearCache(String cacheAlias, Class<K> keyType, Class<V> valueType) {
        if (cacheAlias == null || cacheAlias.isBlank()) {
            return;
        }

        var cache = CACHE_MANAGER.getCache(cacheAlias, keyType, valueType);
        if (cache != null) {
            cache.clear();
        }
    }

    public static void clearCacheEntry(Integer customerId) {
        clearCacheEntry(LOOKUP_CACHE_ALIAS, Integer.class, LookupCache.class, customerId);
    }

    private static <K, V> void clearCacheEntry(String cacheAlias, Class<K> keyType, Class<V> valueType, K key) {
        if (cacheAlias == null || cacheAlias.isBlank() || key == null) {
            return;
        }

        var cache = CACHE_MANAGER.getCache(cacheAlias, keyType, valueType);
        if (cache != null) {
            cache.remove(key);
        }
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
