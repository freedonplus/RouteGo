package com.rg.customer.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rg.common.crypto.SensitiveCipher;
import com.rg.common.web.PageResult;
import com.rg.customer.dto.CustomerQuery;
import com.rg.customer.entity.Customer;
import com.rg.customer.mapper.CouponMapper;
import com.rg.customer.mapper.CustomerMapper;
import com.rg.customer.mapper.model.CustomerCouponCount;
import com.rg.customer.service.impl.CustomerServiceImpl;
import com.rg.customer.vo.CustomerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会员列表的关键词检索：昵称/真实姓名模糊匹配，完整手机号额外走盲索引等值匹配。
 * <p>
 * 手机号在库里是 AES 密文，只有盲索引列能等值比对，所以「关键词里带手机号」必须额外拼条件，
 * 否则后台搜索框里输手机号永远查不到人——发券时批量选会员就是被这一点卡住的。
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private SensitiveCipher cipher;
    @Mock
    private PasswordEncoder passwordEncoder;

    private CustomerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CustomerServiceImpl(customerMapper, couponMapper, cipher, passwordEncoder);
        when(customerMapper.selectPage(any(), any())).thenReturn(new Page<>());
    }

    @Test
    @DisplayName("关键词是完整手机号时额外算盲索引")
    void keywordWithFullPhoneMatchesBlindIndex() {
        when(cipher.blindIndex("13800138000")).thenReturn("BLIND-INDEX");

        PageResult<CustomerVO> result = service.page(query("13800138000"));

        verify(cipher).blindIndex("13800138000");
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("关键词不是完整手机号时不碰盲索引")
    void keywordWithoutFullPhoneSkipsBlindIndex() {
        service.page(query("小明"));

        verify(cipher, never()).blindIndex(anyString());
    }

    @Test
    @DisplayName("关键词带首尾空格时按去掉空格后的手机号查")
    void keywordIsTrimmedBeforePhoneMatch() {
        when(cipher.blindIndex("13800138000")).thenReturn("BLIND-INDEX");

        service.page(query("  13800138000  "));

        verify(cipher).blindIndex("13800138000");
    }

    @Test
    @DisplayName("列表页批量填充可用券数量，没券的补 0")
    void pageFillsAvailableCouponCount() {
        CustomerCouponCount count = new CustomerCouponCount();
        count.setCustomerId(1L);
        count.setTotal(3L);
        Page<Customer> page = new Page<>(1, 10);
        page.setRecords(List.of(customer(1L), customer(2L)));
        when(customerMapper.selectPage(any(), any())).thenReturn(page);
        when(couponMapper.countAvailableByCustomers(List.of(1L, 2L))).thenReturn(List.of(count));

        PageResult<CustomerVO> result = service.page(new CustomerQuery());

        assertEquals(3L, result.getRecords().get(0).getAvailableCouponCount());
        assertEquals(0L, result.getRecords().get(1).getAvailableCouponCount());
    }

    private Customer customer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setPhone("cipher");
        customer.setNickname("会员" + id);
        customer.setLevel((byte) 1);
        customer.setStatus((byte) 1);
        return customer;
    }

    private CustomerQuery query(String keyword) {
        CustomerQuery query = new CustomerQuery();
        query.setKeyword(keyword);
        return query;
    }
}
