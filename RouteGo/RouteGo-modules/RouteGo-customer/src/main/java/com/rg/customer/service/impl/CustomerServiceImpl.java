package com.rg.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.CustomerException;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CustomerCreateRequest;
import com.rg.customer.dto.CustomerPasswordRequest;
import com.rg.customer.dto.CustomerPhoneRequest;
import com.rg.customer.dto.CustomerQuery;
import com.rg.customer.dto.CustomerUpdateRequest;
import com.rg.customer.entity.Customer;
import com.rg.customer.enums.CustomerDict;
import com.rg.customer.mapper.CouponMapper;
import com.rg.customer.mapper.CustomerMapper;
import com.rg.customer.service.CustomerService;
import com.rg.customer.vo.CustomerVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员业务实现。
 * <p>
 * 手机号与证件号入口加密、出口脱敏；手机号另存 HMAC 盲索引以支持等值查询，
 * 因为 AES/GCM 每次加密结果都不同，密文列没法直接比对。
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    private final CustomerMapper customerMapper;
    private final CouponMapper couponMapper;
    private final SensitiveCipher cipher;
    private final PasswordEncoder passwordEncoder;

    public CustomerServiceImpl(CustomerMapper customerMapper,
                               CouponMapper couponMapper,
                               SensitiveCipher cipher,
                               PasswordEncoder passwordEncoder) {
        this.customerMapper = customerMapper;
        this.couponMapper = couponMapper;
        this.cipher = cipher;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<CustomerVO> page(CustomerQuery query) {
        LambdaQueryWrapper<Customer> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.eq(Customer::getPhoneIndex, cipher.blindIndex(query.getPhone().trim()));
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Customer::getNickname, keyword).or().like(Customer::getRealName, keyword));
        }
        if (query.getLevel() != null) {
            wrapper.eq(Customer::getLevel, query.getLevel());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Customer::getStatus, query.getStatus());
        }
        if (query.getRegisterTimeStart() != null) {
            wrapper.ge(Customer::getRegisterTime, query.getRegisterTimeStart());
        }
        if (query.getRegisterTimeEnd() != null) {
            wrapper.le(Customer::getRegisterTime, query.getRegisterTimeEnd());
        }
        wrapper.orderByDesc(Customer::getRegisterTime).orderByDesc(Customer::getId);
        Page<Customer> page = customerMapper.selectPage(query.toPage(), wrapper);
        List<CustomerVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize(), page.getPages());
    }

    @Override
    public CustomerVO detail(Long id) {
        Customer customer = requireCustomer(id);
        CustomerVO vo = toVO(customer);
        vo.setAvailableCouponCount(couponMapper.countByCustomerAndStatus(id, 0));
        vo.setUsedCouponCount(couponMapper.countByCustomerAndStatus(id, 1));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerCreateRequest request) {
        String phone = request.getPhone().trim();
        String phoneIndex = cipher.blindIndex(phone);
        if (customerMapper.countByPhoneIndexIncludingDeleted(phoneIndex) > 0) {
            throw new CustomerException(ErrorCode.CONFLICT, "该手机号已注册（可能已被删除），请先恢复或换绑");
        }
        Customer customer = new Customer();
        customer.setPhone(cipher.encrypt(phone));
        customer.setPhoneIndex(phoneIndex);
        customer.setPassword(StringUtils.hasText(request.getPassword())
                ? passwordEncoder.encode(request.getPassword())
                : null);
        customer.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : maskNickname(phone));
        customer.setAvatar(request.getAvatar());
        customer.setRealName(request.getRealName());
        customer.setIdCard(cipher.encrypt(request.getIdCard()));
        customer.setGender(request.getGender());
        customer.setBirthday(request.getBirthday());
        customer.setEmail(request.getEmail());
        customer.setLevel(request.getLevel() == null ? (byte) 1 : request.getLevel());
        customer.setPoints(request.getPoints() == null ? 0 : request.getPoints());
        customer.setOrderCount(0);
        customer.setTotalAmount(BigDecimal.ZERO);
        customer.setStatus(request.getStatus() == null ? (byte) 1 : request.getStatus());
        customer.setRegisterTime(LocalDateTime.now());
        customerMapper.insert(customer);
        return customer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CustomerUpdateRequest request) {
        requireCustomer(id);
        Customer update = new Customer();
        update.setId(id);
        if (request.getNickname() != null) {
            update.setNickname(request.getNickname().trim());
        }
        update.setAvatar(request.getAvatar());
        update.setRealName(request.getRealName());
        if (request.getIdCard() != null) {
            // 空字符串表示清空证件号
            update.setIdCard(request.getIdCard().isEmpty() ? "" : cipher.encrypt(request.getIdCard().trim()));
        }
        update.setGender(request.getGender());
        update.setBirthday(request.getBirthday());
        update.setEmail(request.getEmail());
        customerMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePhone(Long id, CustomerPhoneRequest request) {
        requireCustomer(id);
        String phone = request.getPhone().trim();
        String phoneIndex = cipher.blindIndex(phone);
        if (customerMapper.countByPhoneIndexExcludingId(phoneIndex, id) > 0) {
            throw new CustomerException(ErrorCode.CONFLICT, "该手机号已被其他会员使用");
        }
        Customer update = new Customer();
        update.setId(id);
        update.setPhone(cipher.encrypt(phone));
        update.setPhoneIndex(phoneIndex);
        customerMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, CustomerPasswordRequest request) {
        requireCustomer(id);
        Customer update = new Customer();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(request.getPassword()));
        customerMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Byte status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "状态只能是 0（禁用）或 1（正常）");
        }
        requireCustomer(id);
        Customer update = new Customer();
        update.setId(id);
        update.setStatus(status);
        customerMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLevel(Long id, Byte level) {
        if (level == null || level < 1 || level > 4) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "会员等级只能是 1（普通）/2（银卡）/3（金卡）/4（钻石）");
        }
        requireCustomer(id);
        Customer update = new Customer();
        update.setId(id);
        update.setLevel(level);
        customerMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long id, Integer delta) {
        if (delta == null || delta == 0) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "积分调整值不能为空或 0");
        }
        requireCustomer(id);
        customerMapper.adjustPoints(id, delta);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireCustomer(id);
        // 会员删除后其未使用的券不再可用，先作废再删会员，避免券包出现指向已删会员的可用券
        couponMapper.expireUnusedByCustomer(id);
        customerMapper.deleteById(id);
    }

    @Override
    public CustomerVO findByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        Customer customer = customerMapper.selectByPhoneIndex(cipher.blindIndex(phone.trim()));
        return customer == null ? null : toVO(customer);
    }

    @Override
    public Long registerByPhone(String phone) {
        CustomerCreateRequest request = new CustomerCreateRequest();
        request.setPhone(phone.trim());
        // 验证码注册不设密码，后续可在「账号安全」里补设
        return create(request);
    }

    @Override
    public CustomerVO loginByPassword(String phone, String rawPassword) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(rawPassword)) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "请输入手机号和密码");
        }
        Customer customer = customerMapper.selectByPhoneIndex(cipher.blindIndex(phone.trim()));
        if (customer == null || !StringUtils.hasText(customer.getPassword())) {
            // 不区分「未注册」和「未设密码」，避免暴露账号是否存在
            throw new CustomerException(ErrorCode.BAD_REQUEST, "账号尚未设置密码，请使用验证码登录");
        }
        if (customer.getStatus() != null && customer.getStatus() != 1) {
            throw new CustomerException(ErrorCode.FORBIDDEN, "账号已被禁用，请联系客服");
        }
        if (!passwordEncoder.matches(rawPassword, customer.getPassword())) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "手机号或密码不正确");
        }
        touchLogin(customer);
        return toVO(customer);
    }

    @Override
    public CustomerVO findByAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String value = account.trim();
        // 11 位以 1 开头 = 手机号，其余当用户名（昵称）处理
        if (value.matches("^1\\d{10}$")) {
            return findByPhone(value);
        }
        List<Customer> matches = customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getNickname, value)
                .eq(Customer::getDeleted, (byte) 0)
                .orderByAsc(Customer::getId)
                .last("LIMIT 2"));
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "该用户名对应多个账号，请改用手机号登录");
        }
        return toVO(matches.get(0));
    }

    @Override
    public CustomerVO loginByAccount(String account, String rawPassword) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(rawPassword)) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "请输入账号和密码");
        }
        CustomerVO matched = findByAccount(account);
        if (matched == null) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "账号或密码不正确");
        }
        Customer customer = customerMapper.selectById(matched.getId());
        if (customer == null) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "账号或密码不正确");
        }
        if (!StringUtils.hasText(customer.getPassword())) {
            // 验证码登录自动建的号没有密码，这里明确引导补设，避免用户以为密码错了
            throw new CustomerException(ErrorCode.BAD_REQUEST, "该账号尚未设置密码，请用验证码登录后再设置密码");
        }
        if (customer.getStatus() != null && customer.getStatus() != 1) {
            throw new CustomerException(ErrorCode.FORBIDDEN, "账号已被禁用，请联系客服");
        }
        if (!passwordEncoder.matches(rawPassword, customer.getPassword())) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "账号或密码不正确");
        }
        touchLogin(customer);
        return toVO(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNickname(Long id, String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return;
        }
        String value = nickname.trim();
        if (value.length() < 2 || value.length() > 20) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "用户名需 2-20 个字符");
        }
        Long exists = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getNickname, value)
                .eq(Customer::getDeleted, (byte) 0)
                .ne(Customer::getId, id));
        if (exists != null && exists > 0) {
            throw new CustomerException(ErrorCode.CONFLICT, "该用户名已被使用，换一个试试");
        }
        Customer update = new Customer();
        update.setId(id);
        update.setNickname(value);
        customerMapper.updateById(update);
    }

    @Override
    public boolean hasPassword(Long id) {
        Customer customer = customerMapper.selectById(id);
        return customer != null && StringUtils.hasText(customer.getPassword());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensurePassword(Long id, String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new CustomerException(ErrorCode.BAD_REQUEST, "密码不能为空");
        }
        Customer customer = requireCustomer(id);
        if (StringUtils.hasText(customer.getPassword())
                && passwordEncoder.matches(rawPassword, customer.getPassword())) {
            // 密码与现值相同也视为设置成功，保持幂等
            return;
        }
        Customer update = new Customer();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(rawPassword));
        customerMapper.updateById(update);
    }

    /** 补写最后登录时间，失败不影响登录主流程 */
    private Customer touchLogin(Customer customer) {
        Customer update = new Customer();
        update.setId(customer.getId());
        update.setLastLoginTime(LocalDateTime.now());
        customerMapper.updateById(update);
        return customer;
    }

    /**
     * 取会员，不存在直接 404。
     */
    private Customer requireCustomer(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new CustomerException(ErrorCode.NOT_FOUND, "会员不存在");
        }
        return customer;
    }

    private CustomerVO toVO(Customer customer) {
        CustomerVO vo = new CustomerVO();
        vo.setId(customer.getId());
        vo.setPhone(cipher.maskPhone(customer.getPhone()));
        vo.setNickname(customer.getNickname());
        vo.setAvatar(customer.getAvatar());
        vo.setRealName(customer.getRealName());
        vo.setIdCard(cipher.maskIdCard(customer.getIdCard()));
        vo.setGender(customer.getGender());
        vo.setGenderDesc(CustomerDict.genderDesc(customer.getGender()));
        vo.setBirthday(customer.getBirthday());
        vo.setEmail(customer.getEmail());
        vo.setLevel(customer.getLevel());
        vo.setLevelDesc(CustomerDict.levelDesc(customer.getLevel()));
        vo.setPoints(customer.getPoints());
        vo.setOrderCount(customer.getOrderCount());
        vo.setTotalAmount(customer.getTotalAmount());
        vo.setStatus(customer.getStatus());
        vo.setStatusDesc(CustomerDict.customerStatusDesc(customer.getStatus()));
        vo.setWechatBound(StringUtils.hasText(customer.getOpenid()));
        vo.setRegisterTime(customer.getRegisterTime());
        vo.setLastLoginTime(customer.getLastLoginTime());
        return vo;
    }

    /**
     * 未填昵称时用手机号尾号兜底，避免列表里一片空白。
     */
    private String maskNickname(String phone) {
        String tail = phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        return "用户" + tail;
    }
}
