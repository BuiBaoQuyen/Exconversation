package bbq.excon.exconversationbackend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@SuppressWarnings("null")
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        // Create Caffeine cache
        com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(3600, TimeUnit.SECONDS)
                .recordStats()
                .build();
        
        // Wrap in Spring Cache interface
        Cache springCache = new Cache() {
            @Override
            public String getName() {
                return "geminiResponses";
            }
            
            @Override
            public Object getNativeCache() {
                return caffeineCache;
            }
            
            @Override
            public Cache.ValueWrapper get(Object key) {
                Object value = caffeineCache.getIfPresent(key);
                return value != null ? new SimpleValueWrapper(value) : null;
            }
            
            @Override
            public <T> T get(Object key, Class<T> type) {
                Object value = caffeineCache.getIfPresent(key);
                return type != null && type.isInstance(value) ? type.cast(value) : null;
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public <T> T get(Object key, Callable<T> valueLoader) {
                return (T) caffeineCache.get(key, k -> {
                    try {
                        return valueLoader.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            
            @Override
            public void put(Object key, Object value) {
                caffeineCache.put(key, value);
            }
            
            @Override
            public void evict(Object key) {
                caffeineCache.invalidate(key);
            }
            
            @Override
            public void clear() {
                caffeineCache.invalidateAll();
            }
        };
        
        cacheManager.setCaches(Arrays.asList(springCache));
        return cacheManager;
    }
}

