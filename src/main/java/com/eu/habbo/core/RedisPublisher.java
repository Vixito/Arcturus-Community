package com.eu.habbo.core;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;

/**
 * Singleton Redis publisher used to notify external services (e.g. CMS)
 * about changes that should trigger real-time UI updates.
 *
 * The connection details are read from the emulator's config.ini / emulator_settings:
 *   redis.host  (default: redis)
 *   redis.port  (default: 6379)
 */
public class RedisPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPublisher.class);

    private static RedisPublisher instance;
    private JedisPool pool;
    private boolean enabled;

    private RedisPublisher() {
        try {
            String host = Emulator.getConfig().getValue("redis.host", "redis");
            int port = Emulator.getConfig().getInt("redis.port", 6379);

            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(4);
            poolConfig.setMaxIdle(2);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);

            this.pool = new JedisPool(poolConfig, host, port);
            this.enabled = true;
            LOGGER.info("[RedisPublisher] Connected to Redis at {}:{}", host, port);
        } catch (Exception e) {
            this.enabled = false;
            LOGGER.warn("[RedisPublisher] Could not connect to Redis. Real-time leaderboards disabled.", e);
        }
    }

    public static synchronized RedisPublisher getInstance() {
        if (instance == null) {
            instance = new RedisPublisher();
        }
        return instance;
    }

    /**
     * Publishes a message to a Redis channel. Fire-and-forget.
     * @param channel the channel name (e.g. "leaderboards_update")
     * @param message the message payload (can be empty string)
     */
    public void publish(String channel, String message) {
        if (!this.enabled || this.pool == null) return;

        Emulator.getThreading().run(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, message);
            } catch (Exception e) {
                LOGGER.warn("[RedisPublisher] Failed to publish to channel '{}': {}", channel, e.getMessage());
            }
        });
    }

    /**
     * Convenience method to notify the CMS that leaderboard data has changed.
     */
    public void notifyLeaderboardUpdate() {
        this.publish("leaderboards_update", "refresh");
    }

    public void dispose() {
        if (this.pool != null && !this.pool.isClosed()) {
            this.pool.close();
            LOGGER.info("[RedisPublisher] Redis connection pool closed.");
        }
    }
}
