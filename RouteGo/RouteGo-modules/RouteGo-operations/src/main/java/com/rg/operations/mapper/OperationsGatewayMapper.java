package com.rg.operations.mapper;

import com.rg.operations.mapper.model.CustomerSnapshot;
import com.rg.operations.mapper.model.OrderSnapshot;
import com.rg.operations.mapper.model.UserSnapshot;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 运营域对 customer / trade / admin 域的只读网关。
 * <p>
 * 模块化单体共用一个库，但跨域写入口必须留在各自模块。这里只放三件事：
 * 列表页补昵称、补订单号、把 handler_id 翻译成人名 —— 全是只读快照查询。
 * <p>
 * 一律带 {@code deleted = 0}：历史工单里引用的会员可能已被删除，
 * 查不到就显示空，不要因为关联对象不存在让整页 500。
 */
public interface OperationsGatewayMapper {

    @Select("""
            SELECT id, nickname, phone, status
            FROM customer
            WHERE id = #{id} AND deleted = 0
            """)
    CustomerSnapshot selectCustomer(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, nickname, phone, status
            FROM customer
            WHERE deleted = 0 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<CustomerSnapshot> selectCustomers(@Param("ids") Collection<Long> ids);

    @Select("""
            SELECT id, order_no, customer_id, status, pay_amount
            FROM orders
            WHERE id = #{id} AND deleted = 0
            """)
    OrderSnapshot selectOrder(@Param("id") Long id);

    @Select("""
            <script>
            SELECT id, order_no, customer_id, status, pay_amount
            FROM orders
            WHERE deleted = 0 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<OrderSnapshot> selectOrders(@Param("ids") Collection<Long> ids);

    @Select("""
            <script>
            SELECT id, username, nickname
            FROM sys_user
            WHERE deleted = 0 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<UserSnapshot> selectUsers(@Param("ids") Collection<Long> ids);

    /**
     * 只返回「未删除且启用」的账号，用于自动分配时剔除离职/停用的客服。
     */
    @Select("""
            <script>
            SELECT id, username, nickname
            FROM sys_user
            WHERE deleted = 0 AND status = 1 AND id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<UserSnapshot> selectEnabledUsers(@Param("ids") Collection<Long> ids);

    /**
     * 可指派的处理人候选人：拥有后台角色的账号（排除 App 端会员角色），账号需正常。
     * <p>
     * 直接查用户角色，而不是拿 {@code admin:SysUser:page} 的全量用户列表来筛——
     * {@code sys_user} 里同时存着 C 端注册账号（normal_user / member_user），
     * 直接全量拉出来会让「处理人」下拉里混进一堆会员，而且只能按用户名搜，搜昵称搜不到，
     * 看起来就像「筛选不了客服」。客服角色的账号排在最前。
     */
    @Select("""
            <script>
            SELECT u.id, u.username, u.nickname
            FROM sys_user u
            JOIN sys_user_role ur ON ur.user_id = u.id
            JOIN sys_role r ON r.id = ur.role_id
            WHERE u.deleted = 0 AND u.status = 1
              AND r.deleted = 0 AND r.status = 1
              AND r.role_code NOT IN ('normal_user', 'member_user')
            <if test="keyword != null and keyword != ''">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            GROUP BY u.id, u.username, u.nickname
            ORDER BY MAX(r.role_code = 'support') DESC, u.id
            LIMIT 50
            </script>
            """)
    List<UserSnapshot> selectSupportUsers(@Param("keyword") String keyword);
}
