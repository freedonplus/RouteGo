package com.rg.operations.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.operations.dto.CustomQuoteCreateRequest;
import com.rg.operations.dto.CustomQuoteUpdateRequest;
import com.rg.operations.entity.CustomQuote;
import com.rg.operations.entity.CustomRequirement;
import com.rg.operations.mapper.CustomQuoteMapper;
import com.rg.operations.mapper.CustomRequirementMapper;
import com.rg.operations.service.impl.CustomQuoteServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 定制报价的核心规则：采纳只能成功一次、同需求其它报价自动失效、需求同步成交。
 * <p>
 * 这里用纯 Mockito 打桩，不起 Spring 上下文；状态推进都是条件 UPDATE，
 * 因此断言落在「更新语句被调用了多少次」和「异常类型」上。
 */
@ExtendWith(MockitoExtension.class)
class CustomQuoteServiceImplTest {

    @Mock
    private CustomQuoteMapper quoteMapper;
    @Mock
    private CustomRequirementMapper requirementMapper;

    private CustomQuoteServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CustomQuote.class);
        TableInfoHelper.initTableInfo(assistant, CustomRequirement.class);
    }

    @BeforeEach
    void setUp() {
        service = new CustomQuoteServiceImpl(quoteMapper, requirementMapper);
    }

    @Test
    @DisplayName("采纳报价：本单置已采纳、其它待确认作废、需求置已成交")
    void acceptMarksQuoteAndRequirement() {
        when(quoteMapper.selectById(1L)).thenReturn(quote(1L, 10L, (byte) 0));
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 1, 0));
        when(quoteMapper.update(isNull(), any())).thenReturn(1);
        when(requirementMapper.update(isNull(), any())).thenReturn(1);

        service.accept(1L);

        // 两次报价更新：本单置已采纳 + 同需求其它待确认置失效
        verify(quoteMapper, times(2)).update(isNull(), any());
        verify(requirementMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("重复采纳同一版报价：冲突")
    void acceptTwiceIsRejected() {
        when(quoteMapper.selectById(1L)).thenReturn(quote(1L, 10L, (byte) 1));
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 2, 0));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.accept(1L));
        assertTrue(ex.getMessage().contains("已被采纳"));
        verify(quoteMapper, never()).update(isNull(), any());
    }

    @Test
    @DisplayName("采纳已失效的报价：冲突")
    void acceptInvalidQuoteIsRejected() {
        when(quoteMapper.selectById(1L)).thenReturn(quote(1L, 10L, (byte) 2));
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 1, 0));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.accept(1L));
        assertTrue(ex.getMessage().contains("已失效"));
    }

    @Test
    @DisplayName("需求已关闭时不能采纳报价")
    void acceptOnClosedRequirementIsRejected() {
        when(quoteMapper.selectById(1L)).thenReturn(quote(1L, 10L, (byte) 0));
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 3, 0));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.accept(1L));
        assertTrue(ex.getMessage().contains("已关闭"));
    }

    @Test
    @DisplayName("已成交的需求不能继续报价")
    void createOnDealRequirementIsRejected() {
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 2, 0));

        OperationsException ex = assertThrows(OperationsException.class,
                () -> service.create(createRequest(10L)));
        assertTrue(ex.getMessage().contains("已成交"));
        verify(quoteMapper, never()).insert(any(CustomQuote.class));
    }

    @Test
    @DisplayName("首版报价把待跟进的需求推进到已报价")
    void createAdvancesRequirementToQuoted() {
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 0, 0));
        when(quoteMapper.countByQuoteNoIncludingDeleted(any())).thenReturn(0);
        when(quoteMapper.insert(any(CustomQuote.class))).thenReturn(1);
        when(requirementMapper.update(isNull(), any())).thenReturn(1);

        service.create(createRequest(10L));

        verify(quoteMapper).insert(any(CustomQuote.class));
        verify(requirementMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("已采纳的报价不能修改，调价需新建一版")
    void updateAcceptedQuoteIsRejected() {
        when(quoteMapper.selectById(1L)).thenReturn(quote(1L, 10L, (byte) 1));
        CustomQuoteUpdateRequest request = new CustomQuoteUpdateRequest();
        request.setAmount(new BigDecimal("100.00"));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.update(1L, request));
        assertTrue(ex.getMessage().contains("待确认"));
        verify(quoteMapper, never()).updateById(any(CustomQuote.class));
    }

    @Test
    @DisplayName("已采纳的报价不能作废")
    void invalidateAcceptedQuoteIsRejected() {
        when(quoteMapper.selectById(1L)).thenReturn(quote(1L, 10L, (byte) 1));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.invalidate(1L));
        assertTrue(ex.getMessage().contains("成交凭据"));
    }

    @Test
    @DisplayName("有效期早于今天直接拒绝")
    void createWithPastValidUntilIsRejected() {
        when(requirementMapper.selectById(10L)).thenReturn(requirement(10L, (byte) 1, 0));
        CustomQuoteCreateRequest request = createRequest(10L);
        request.setValidUntil(LocalDate.now().minusDays(1));

        OperationsException ex = assertThrows(OperationsException.class, () -> service.create(request));
        assertTrue(ex.getMessage().contains("有效期"));
    }

    // ──────────────────────────────────────────────
    // 测试数据
    // ──────────────────────────────────────────────

    private CustomQuote quote(Long id, Long requirementId, byte status) {
        CustomQuote quote = new CustomQuote();
        quote.setId(id);
        quote.setRequirementId(requirementId);
        quote.setQuoteNo("CQ2026110500001");
        quote.setAmount(new BigDecimal("6800.00"));
        quote.setStatus(status);
        return quote;
    }

    private CustomRequirement requirement(Long id, byte status, int version) {
        CustomRequirement requirement = new CustomRequirement();
        requirement.setId(id);
        requirement.setRequirementNo("CR2026110500001");
        requirement.setContactName("张三");
        requirement.setDestination("云南");
        requirement.setStatus(status);
        requirement.setVersion(version);
        return requirement;
    }

    private CustomQuoteCreateRequest createRequest(Long requirementId) {
        CustomQuoteCreateRequest request = new CustomQuoteCreateRequest();
        request.setRequirementId(requirementId);
        request.setAmount(new BigDecimal("6800.00"));
        return request;
    }
}
