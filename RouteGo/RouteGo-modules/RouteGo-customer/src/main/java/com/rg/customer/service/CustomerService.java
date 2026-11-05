package com.rg.customer.service;

import com.rg.common.web.PageResult;
import com.rg.customer.dto.CustomerCreateRequest;
import com.rg.customer.dto.CustomerPasswordRequest;
import com.rg.customer.dto.CustomerPhoneRequest;
import com.rg.customer.dto.CustomerQuery;
import com.rg.customer.dto.CustomerUpdateRequest;
import com.rg.customer.vo.CustomerVO;

/**
 * 会员服务。
 * <p>
 * 手机号与证件号在出口一律脱敏、在入口一律加密，调用方不需要关心加解密细节。
 * 会员资产相关字段（积分、累计消费、订单数）只能由交易流程或本服务的积分接口调整，
 * 资料编辑接口不会碰它们。
 */
public interface CustomerService {

    PageResult<CustomerVO> page(CustomerQuery query);

    /**
     * 会员详情，附带可用券与已用券数量。
     */
    CustomerVO detail(Long id);

    /**
     * 后台代注册会员，手机号唯一。
     */
    Long create(CustomerCreateRequest request);

    /**
     * 修改资料。手机号不在此修改，见 {@link #updatePhone}。
     */
    void update(Long id, CustomerUpdateRequest request);

    /**
     * 手机号换绑：同时改密文列与盲索引列。
     */
    void updatePhone(Long id, CustomerPhoneRequest request);

    /**
     * 重置登录密码（BCrypt）。
     */
    void resetPassword(Long id, CustomerPasswordRequest request);

    /**
     * 启用/禁用。禁用后会员无法登录，已领取的券不受影响。
     */
    void updateStatus(Long id, Byte status);

    /**
     * 调整会员等级：1 普通，2 银卡，3 金卡，4 钻石。
     */
    void updateLevel(Long id, Byte level);

    /**
     * 调整积分（可为负），结果不会小于 0。
     */
    void adjustPoints(Long id, Integer delta);

    /**
     * 删除会员（逻辑删除），未使用的券一并作废。
     */
    void delete(Long id);

    /**
     * C 端：按手机号查会员，未注册返回 {@code null}。
     * <p>
     * 手机号在库里是密文，这里走 HMAC 盲索引等值查询，不做全表解密。
     */
    CustomerVO findByPhone(String phone);

    /**
     * C 端：手机号静默注册，仅建号不设密码，返回会员 ID。
     */
    Long registerByPhone(String phone);

    /**
     * C 端：手机号 + 密码登录。密码用 BCrypt 校验；未设过密码的账号
     * 提示走验证码登录，禁用账号拒绝登录。
     */
    CustomerVO loginByPassword(String phone, String rawPassword);

    /**
     * C 端：为已有会员设置/重置密码（验证码校验通过后由 controller 调用）。
     */
    void ensurePassword(Long id, String rawPassword);

    /**
     * C 端：按「账号」查会员。账号为 11 位手机号时走盲索引，
     * 否则按用户名（昵称）精确匹配；用户名命中多个账号会直接报错要求改用手机号。
     */
    CustomerVO findByAccount(String account);

    /**
     * C 端：账号密码登录。账号可为手机号或用户名，密码用 BCrypt 校验。
     * 账号不存在与密码错误返回同一句提示，避免暴露账号是否注册。
     */
    CustomerVO loginByAccount(String account, String rawPassword);

    /**
     * C 端：设置用户名（昵称）。用作登录账号，2-20 位且全局唯一。
     */
    void setNickname(Long id, String nickname);

    /**
     * C 端：是否已设置登录密码。未设置的账号只能用验证码登录。
     */
    boolean hasPassword(Long id);
}
