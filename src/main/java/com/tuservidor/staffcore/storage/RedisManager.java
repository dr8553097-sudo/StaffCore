package com.tuservidor.staffcore.storage;

import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RedisManager implements AutoCloseable {

    private final Logger logger;
    private final boolean enabled;
    private final String channelPrefix;
    private JedisPool jedisPool;
    private Thread subscriberThread;
    private JedisPubSub pubSubListener;
    private final Map<String, Consumer<String>> channelListeners = new ConcurrentHashMap<>();

    public RedisManager(FileConfiguration config, Logger logger) {
        this.logger = logger;
        this.enabled = config.getBoolean("redis.enabled", false);
        this.channelPrefix = config.getString("redis.channel-prefix", "staffcore:");

        if (enabled) {
            initPool(config);
            startSubscriber();
        }
    }

    private void initPool(FileConfiguration config) {
        String host = config.getString("redis.host", "127.0.0.1");
        int port = config.getInt("redis.port", 6379);
        String password = config.getString("redis.password", "");
        int timeout = config.getInt("redis.timeout-ms", 3000);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);

        try {
            if (password != null && !password.isBlank()) {
                this.jedisPool = new JedisPool(poolConfig, host, port, timeout, password);
            } else {
                this.jedisPool = new JedisPool(poolConfig, host, port, timeout);
            }
            logger.info("[StaffCore] Connected to Redis at " + host + ":" + port);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[StaffCore] Failed to connect to Redis: " + e.getMessage(), e);
        }
    }

    private void startSubscriber() {
        if (jedisPool == null) return;

        this.pubSubListener = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                Consumer<String> handler = channelListeners.get(channel);
                if (handler != null) {
                    try {
                        handler.accept(message);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "[StaffCore] Error handling Redis message on channel " + channel + ": " + e.getMessage(), e);
                    }
                }
            }
        };

        this.subscriberThread = new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.psubscribe(pubSubListener, channelPrefix + "*");
            } catch (Exception e) {
                if (enabled) {
                    logger.log(Level.WARNING, "[StaffCore] Redis subscriber disconnected: " + e.getMessage());
                }
            }
        }, "StaffCore-Redis-Subscriber");
        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
    }

    public void registerChannel(String subChannel, Consumer<String> handler) {
        if (!enabled) return;
        channelListeners.put(channelPrefix + subChannel, handler);
    }

    public void publish(String subChannel, String message) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(channelPrefix + subChannel, message);
        } catch (Exception e) {
            logger.log(Level.WARNING, "[StaffCore] Failed to publish Redis message to " + subChannel + ": " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled && jedisPool != null && !jedisPool.isClosed();
    }

    @Override
    public void close() {
        if (pubSubListener != null && pubSubListener.isSubscribed()) {
            try {
                pubSubListener.punsubscribe();
            } catch (Exception ignored) {}
        }
        if (subscriberThread != null && subscriberThread.isAlive()) {
            subscriberThread.interrupt();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
            logger.info("[StaffCore] Redis connection pool closed.");
        }
    }
}
