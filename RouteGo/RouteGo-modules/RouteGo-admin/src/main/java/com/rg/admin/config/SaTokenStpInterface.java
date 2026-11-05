package com.rg.admin.config;

import cn.dev33.satoken.stp.StpInterface;
import com.rg.admin.mapper.SysPermissionMapper;
import com.rg.admin.mapper.SysRoleMapper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.redis.RedisService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 角色/权限数据源。
 * <p>
 * 权限码按用户缓存：key = {@code PERM_LIST(userId, version)}，version 是
 * {@link CacheKeyConstants#Admin} 中的全局版本号。所有会改变权限数据的写接口
 * 只需删除版本号 key，即可一次性失效所有用户的权限缓存，避免脏读。
 * 只有 super_admin 拥有通配权限 {@code *}。
 */
@Component
public class SaTokenStpInterface implements StpInterface {

    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String DEFAULT_VERSION = "0";
    private static final Duration PERM_CACHE_TTL = Duration.ofMinutes(10);

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final RedisService redisService;

    public SaTokenStpInterface(SysRoleMapper sysRoleMapper,
                               SysPermissionMapper sysPermissionMapper,
                               RedisService redisService) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.redisService = redisService;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        String version = permCacheVersion(loginId);
        String cacheKey = permCacheKey(loginId, version);
// 容错：历史脏数据反序列化失败时删除并回源，避免接口 500
        List<String> cached;
        try {
            cached = redisService.get(cacheKey);
        } catch (org.springframework.data.redis.serializer.SerializationException e) {
            redisService.delete(cacheKey);
            cached = null;
        }
        if (cached != null) {
            return cached;
        }

        List<String> roles = getRoleList(loginId, loginType);
        List<String> perms;
        if (roles.contains(SUPER_ADMIN_ROLE)) {
            // 必须用可变 ArrayList：不可变 List 是 final 类，GenericJackson2JsonRedisSerializer
            // 序列化时不写类型标识，读回时会报 InvalidTypeIdException
            perms = new ArrayList<>(List.of("*"));
        } else {
            perms = new ArrayList<>(sysPermissionMapper.selectPermissionCodesByUserId(asUserId(loginId)));
        }
        redisService.set(cacheKey, perms, PERM_CACHE_TTL);
        return perms;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return sysRoleMapper.selectRoleCodesByUserId(asUserId(loginId));
    }

    private Long asUserId(Object loginId) {
        return Long.valueOf(String.valueOf(loginId));
    }

    /**
     * 版本号 = 全局版本 + 用户版本。
     * <p>
     * 全局版本用于「角色 / 权限点」这类影响多人的变更；用户版本用于只改一个用户的
     * 角色或状态，避免为了一个人把全站权限缓存都冲掉。两个版本都只自增、不删除。
     */
    private String permCacheVersion(Object loginId) {
        Object global = redisService.get(CacheKeyConstants.Admin.PERM_VERSION);
        Object perUser = redisService.get(String.format(
                CacheKeyConstants.Admin.PERM_USER_VERSION, loginId));
        String globalVersion = global == null ? DEFAULT_VERSION : String.valueOf(global);
        String userVersion = perUser == null ? DEFAULT_VERSION : String.valueOf(perUser);
        return globalVersion + ":" + userVersion;
    }

    private String permCacheKey(Object loginId, String version) {
        return String.format(CacheKeyConstants.Admin.PERM_LIST, loginId, version);
    }
}
