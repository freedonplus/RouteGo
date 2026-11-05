package com.rg.operations.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rg.common.exception.ErrorCode;
import com.rg.common.exception.serviceException.OperationsException;
import com.rg.common.web.PageResult;
import com.rg.operations.dto.CustomQuoteCreateRequest;
import com.rg.operations.dto.CustomQuoteQuery;
import com.rg.operations.dto.CustomQuoteUpdateRequest;
import com.rg.operations.entity.CustomQuote;
import com.rg.operations.entity.CustomRequirement;
import com.rg.operations.enums.OperationsDict;
import com.rg.operations.mapper.CustomQuoteMapper;
import com.rg.operations.mapper.CustomRequirementMapper;
import com.rg.operations.service.CustomQuoteService;
import com.rg.operations.support.BizNoGenerator;
import com.rg.operations.support.CurrentOperator;
import com.rg.operations.vo.CustomQuoteVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 定制报价业务实现。
 * <p>
 * 状态推进全部用「带原状态谓词的条件 UPDATE」完成：
 * 采纳时 {@code WHERE status = 0}，并发下只有一次能成功，另一次直接 409，
 * 不会出现同一需求两版报价都被采纳的情况。
 */
@Service
public class CustomQuoteServiceImpl extends ServiceImpl<CustomQuoteMapper, CustomQuote> implements CustomQuoteService {

    private static final byte QUOTE_PENDING = 0;
    private static final byte QUOTE_ACCEPTED = 1;
    private static final byte QUOTE_INVALID = 2;

    private static final byte REQUIREMENT_PENDING = 0;
    private static final byte REQUIREMENT_QUOTED = 1;
    private static final byte REQUIREMENT_DEAL = 2;
    private static final byte REQUIREMENT_CLOSED = 3;

    private static final int NO_RETRY = 5;

    private final CustomQuoteMapper quoteMapper;
    private final CustomRequirementMapper requirementMapper;

    public CustomQuoteServiceImpl(CustomQuoteMapper quoteMapper,
                                  CustomRequirementMapper requirementMapper) {
        this.quoteMapper = quoteMapper;
        this.requirementMapper = requirementMapper;
    }

    @Override
    public PageResult<CustomQuoteVO> page(CustomQuoteQuery query) {
        LambdaQueryWrapper<CustomQuote> wrapper = Wrappers.lambdaQuery();
        if (query.getRequirementId() != null) {
            wrapper.eq(CustomQuote::getRequirementId, query.getRequirementId());
        }
        if (StringUtils.hasText(query.getQuoteNo())) {
            wrapper.like(CustomQuote::getQuoteNo, query.getQuoteNo().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(CustomQuote::getStatus, query.getStatus());
        }
        if (Boolean.TRUE.equals(query.getExpiredOnly())) {
            wrapper.eq(CustomQuote::getStatus, QUOTE_PENDING)
                    .isNotNull(CustomQuote::getValidUntil)
                    .lt(CustomQuote::getValidUntil, LocalDate.now());
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(CustomQuote::getCreateTime, query.getCreateTimeStart());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(CustomQuote::getCreateTime, query.getCreateTimeEnd());
        }
        wrapper.orderByDesc(CustomQuote::getCreateTime).orderByDesc(CustomQuote::getId);
        Page<CustomQuote> page = quoteMapper.selectPage(query.toPage(), wrapper);
        return new PageResult<>(toVOs(page.getRecords()), page.getTotal(), page.getCurrent(),
                page.getSize(), page.getPages());
    }

    @Override
    public CustomQuoteVO detail(Long id) {
        return toVOs(List.of(requireQuote(id))).get(0);
    }

    @Override
    public List<CustomQuoteVO> listByRequirement(Long requirementId) {
        if (requirementId == null) {
            throw new OperationsException("需求单 ID 不能为空");
        }
        List<CustomQuote> quotes = quoteMapper.selectList(Wrappers.<CustomQuote>lambdaQuery()
                .eq(CustomQuote::getRequirementId, requirementId)
                .orderByDesc(CustomQuote::getId));
        return toVOs(quotes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomQuoteCreateRequest request) {
        CustomRequirement requirement = requirementMapper.selectById(request.getRequirementId());
        if (requirement == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "定制需求不存在或已删除");
        }
        if (requirement.getStatus() == REQUIREMENT_CLOSED) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已关闭，不能再报价");
        }
        if (requirement.getStatus() == REQUIREMENT_DEAL) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已成交，如需调价请先关闭原需求再新建");
        }
        if (request.getValidUntil() != null && request.getValidUntil().isBefore(LocalDate.now())) {
            throw new OperationsException("报价有效期不能早于今天");
        }

        CustomQuote quote = new CustomQuote();
        quote.setRequirementId(requirement.getId());
        quote.setQuoteNo(nextQuoteNo());
        quote.setAmount(request.getAmount());
        quote.setDetail(request.getDetail());
        quote.setValidUntil(request.getValidUntil());
        quote.setStatus(QUOTE_PENDING);
        quote.setCreateBy(CurrentOperator.id());
        quote.setCreateTime(LocalDateTime.now());
        quoteMapper.insert(quote);

        // 首版报价把需求推进到「已报价」；已经是已报价/已成交则不动
        if (requirement.getStatus() == REQUIREMENT_PENDING) {
            requirementMapper.update(null, Wrappers.<CustomRequirement>lambdaUpdate()
                    .eq(CustomRequirement::getId, requirement.getId())
                    .eq(CustomRequirement::getVersion, requirement.getVersion())
                    .set(CustomRequirement::getStatus, REQUIREMENT_QUOTED)
                    .set(CustomRequirement::getUpdateBy, CurrentOperator.id())
                    .set(CustomRequirement::getVersion, requirement.getVersion() + 1));
        }
        return quote.getId();
    }

    @Override
    public void update(Long id, CustomQuoteUpdateRequest request) {
        CustomQuote quote = requireQuote(id);
        if (quote.getStatus() != QUOTE_PENDING) {
            throw new OperationsException(ErrorCode.CONFLICT, "只有待确认的报价可以修改，已采纳或已失效的报价需新建一版");
        }
        if (request.getAmount() == null && request.getDetail() == null && request.getValidUntil() == null) {
            throw new OperationsException("没有需要修改的内容");
        }
        if (request.getValidUntil() != null && request.getValidUntil().isBefore(LocalDate.now())) {
            throw new OperationsException("报价有效期不能早于今天");
        }
        CustomQuote update = new CustomQuote();
        update.setId(id);
        update.setAmount(request.getAmount());
        update.setDetail(request.getDetail());
        update.setValidUntil(request.getValidUntil());
        quoteMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long id) {
        CustomQuote quote = requireQuote(id);
        CustomRequirement requirement = requirementMapper.selectById(quote.getRequirementId());
        if (requirement == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "所属定制需求不存在或已删除");
        }
        if (requirement.getStatus() == REQUIREMENT_CLOSED) {
            throw new OperationsException(ErrorCode.CONFLICT, "需求已关闭，不能采纳报价");
        }
        if (quote.getStatus() == QUOTE_ACCEPTED) {
            throw new OperationsException(ErrorCode.CONFLICT, "该报价已被采纳，无需重复操作");
        }
        if (quote.getStatus() == QUOTE_INVALID) {
            throw new OperationsException(ErrorCode.CONFLICT, "该报价已失效，请新建一版再采纳");
        }

        int rows = quoteMapper.update(null, Wrappers.<CustomQuote>lambdaUpdate()
                .eq(CustomQuote::getId, id)
                .eq(CustomQuote::getStatus, QUOTE_PENDING)
                .set(CustomQuote::getStatus, QUOTE_ACCEPTED));
        if (rows == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "报价状态已变化，请刷新后重试");
        }

        // 同需求下其它还挂着的报价一并作废，避免客户手上握着两张都「待确认」的报价
        quoteMapper.update(null, Wrappers.<CustomQuote>lambdaUpdate()
                .eq(CustomQuote::getRequirementId, quote.getRequirementId())
                .eq(CustomQuote::getStatus, QUOTE_PENDING)
                .ne(CustomQuote::getId, id)
                .set(CustomQuote::getStatus, QUOTE_INVALID));

        if (requirement.getStatus() != REQUIREMENT_DEAL) {
            int updated = requirementMapper.update(null, Wrappers.<CustomRequirement>lambdaUpdate()
                    .eq(CustomRequirement::getId, requirement.getId())
                    .eq(CustomRequirement::getVersion, requirement.getVersion())
                    .set(CustomRequirement::getStatus, REQUIREMENT_DEAL)
                    .set(CustomRequirement::getUpdateBy, CurrentOperator.id())
                    .set(CustomRequirement::getVersion, requirement.getVersion() + 1));
            if (updated == 0) {
                throw new OperationsException(ErrorCode.CONFLICT, "需求状态已被其他人修改，请刷新后重试");
            }
        }
    }

    @Override
    public void invalidate(Long id) {
        CustomQuote quote = requireQuote(id);
        if (quote.getStatus() == QUOTE_ACCEPTED) {
            throw new OperationsException(ErrorCode.CONFLICT, "已采纳的报价是成交凭据，不能作废");
        }
        if (quote.getStatus() == QUOTE_INVALID) {
            throw new OperationsException(ErrorCode.CONFLICT, "报价已失效，无需重复操作");
        }
        int rows = quoteMapper.update(null, Wrappers.<CustomQuote>lambdaUpdate()
                .eq(CustomQuote::getId, id)
                .eq(CustomQuote::getStatus, QUOTE_PENDING)
                .set(CustomQuote::getStatus, QUOTE_INVALID));
        if (rows == 0) {
            throw new OperationsException(ErrorCode.CONFLICT, "报价状态已变化，请刷新后重试");
        }
    }

    @Override
    public void delete(Long id) {
        CustomQuote quote = requireQuote(id);
        if (quote.getStatus() != QUOTE_PENDING) {
            throw new OperationsException(ErrorCode.CONFLICT, "只有待确认的报价可以删除，已采纳的报价需保留作为成交凭据");
        }
        quoteMapper.deleteById(id);
    }

    // ──────────────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────────────

    private CustomQuote requireQuote(Long id) {
        CustomQuote quote = quoteMapper.selectById(id);
        if (quote == null) {
            throw new OperationsException(ErrorCode.NOT_FOUND, "报价单不存在");
        }
        return quote;
    }

    private String nextQuoteNo() {
        for (int i = 0; i < NO_RETRY; i++) {
            String no = BizNoGenerator.next(BizNoGenerator.QUOTE_NO_PREFIX);
            if (quoteMapper.countByQuoteNoIncludingDeleted(no) == 0) {
                return no;
            }
        }
        throw new OperationsException(ErrorCode.INTERNAL_ERROR, "报价单号生成失败，请重试");
    }

    private List<CustomQuoteVO> toVOs(List<CustomQuote> quotes) {
        if (quotes.isEmpty()) {
            return List.of();
        }
        Set<Long> requirementIds = quotes.stream()
                .map(CustomQuote::getRequirementId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> requirementNos = requirementIds.isEmpty() ? Map.of()
                : requirementMapper.selectBatchIds(requirementIds).stream()
                .collect(Collectors.toMap(CustomRequirement::getId, CustomRequirement::getRequirementNo));

        LocalDate today = LocalDate.now();
        return quotes.stream().map(quote -> {
            CustomQuoteVO vo = new CustomQuoteVO();
            vo.setId(quote.getId());
            vo.setRequirementId(quote.getRequirementId());
            vo.setRequirementNo(requirementNos.get(quote.getRequirementId()));
            vo.setQuoteNo(quote.getQuoteNo());
            vo.setAmount(quote.getAmount());
            vo.setDetail(quote.getDetail());
            vo.setValidUntil(quote.getValidUntil());
            vo.setStatus(quote.getStatus());
            vo.setStatusDesc(OperationsDict.of(OperationsDict.QUOTE_STATUS, quote.getStatus()));
            vo.setExpired(quote.getStatus() != null && quote.getStatus() == QUOTE_PENDING
                    && quote.getValidUntil() != null && quote.getValidUntil().isBefore(today));
            vo.setCreateBy(quote.getCreateBy());
            vo.setCreateTime(quote.getCreateTime());
            return vo;
        }).toList();
    }
}
