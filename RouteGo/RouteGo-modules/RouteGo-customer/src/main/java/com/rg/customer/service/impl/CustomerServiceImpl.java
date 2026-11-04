package com.rg.customer.service.impl;

import com.rg.customer.entity.Customer;
import com.rg.customer.mapper.CustomerMapper;
import com.rg.customer.service.CustomerService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 会员表 服务实现类
 * </p>
 *
 * @author RouteGo
 * @since 2026-11-03
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

}
