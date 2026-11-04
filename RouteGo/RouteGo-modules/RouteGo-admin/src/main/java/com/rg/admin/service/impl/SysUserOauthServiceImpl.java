package com.rg.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.admin.dto.UserOauthBindRequest;
import com.rg.admin.entity.SysUserOauth;
import com.rg.admin.mapper.SysUserOauthMapper;
import com.rg.admin.service.SysUserOauthService;
import com.rg.admin.vo.SysUserOauthVO;
import com.rg.common.cache.CacheHelper;
import com.rg.common.constant.CacheKeyConstants;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.AdminException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysUserOauthServiceImpl extends ServiceImpl<SysUserOauthMapper, SysUserOauth>
        implements SysUserOauthService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final SysUserOauthMapper oauthMapper;
    private final CacheHelper cacheHelper;

    public SysUserOauthServiceImpl(SysUserOauthMapper oauthMapper, CacheHelper cacheHelper) {
        this.oauthMapper = oauthMapper;
        this.cacheHelper = cacheHelper;
    }

    @Override
    public List<SysUserOauthVO> listByUserId(Long userId) {
        return cacheHelper.getOrLoad(
                String.format(CacheKeyConstants.Admin.OAUTH_BY_USER, userId),
                TTL,
                () -> oauthMapper.selectList(Wrappers.<SysUserOauth>lambdaQuery()
                        .eq(SysUserOauth::getUserId, userId)).stream().map(this::toVO).toList());
    }

    @Override
    @Transactional
    public Long bind(UserOauthBindRequest request) {
        SysUserOauth oauth = new SysUserOauth();
        oauth.setUserId(request.getUserId());
        oauth.setProvider(request.getProvider());
        oauth.setOpenid(request.getOpenid());
        oauth.setUnionid(request.getUnionid());
        oauth.setNickname(request.getNickname());
        oauth.setAvatar(request.getAvatar());
        oauth.setBindTime(LocalDateTime.now());
        try {
            oauthMapper.insert(oauth);
        } catch (DuplicateKeyException e) {
            throw new AdminException(ErrorCode.CONFLICT, "该第三方账号已绑定");
        }
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.OAUTH_BY_USER, request.getUserId()));
        return oauth.getId();
    }

    @Override
    @Transactional
    public void unbind(Long id) {
        SysUserOauth oauth = oauthMapper.selectById(id);
        if (oauth == null) {
            throw new AdminException(ErrorCode.NOT_FOUND, "绑定记录不存在");
        }
        oauthMapper.deleteById(id);
        cacheHelper.evictAfterCommit(
                String.format(CacheKeyConstants.Admin.OAUTH_BY_USER, oauth.getUserId()));
    }

    private SysUserOauthVO toVO(SysUserOauth oauth) {
        SysUserOauthVO vo = new SysUserOauthVO();
        vo.setId(oauth.getId());
        vo.setUserId(oauth.getUserId());
        vo.setProvider(oauth.getProvider());
        vo.setOpenid(oauth.getOpenid());
        vo.setUnionid(oauth.getUnionid());
        vo.setNickname(oauth.getNickname());
        vo.setAvatar(oauth.getAvatar());
        vo.setBindTime(oauth.getBindTime());
        return vo;
    }
}
