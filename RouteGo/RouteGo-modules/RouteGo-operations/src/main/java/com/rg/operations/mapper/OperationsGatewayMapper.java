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
     * 可指派的处理人候选人：后台账号（排除只有 App 会员角色的 C 端账号），账号需正常。
     * <p>
     * 不用「JOIN 角色表再按角色名过滤」那种写法——建号时没勾角色的后台账号会被直接过滤掉，
     * 页面上看起来就是「筛不到人」。这里改成「排除只拥有会员角色的账号」，
     * 没有角色的后台账号依然可选。客服角色排在前面，其余按 id。
     */
    @Select("""
            <script>
            SELECT u.id, u.username, u.nickname
            FROM sys_user u
            WHERE u.deleted = 0 AND u.status = 1
              AND NOT EXISTS (
                  SELECT 1
                  FROM sys_user_role ur
                  JOIN sys_role r ON r.id = ur.role_id AND r.deleted = 0
                  WHERE ur.user_id = u.id
                    AND r.role_code IN ('normal_user', 'member_user')
              )
            <if test="keyword != null and keyword != ''">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY EXISTS (
                SELECT 1
                FROM sys_user_role ur2
                JOIN sys_role r2 ON r2.id = ur2.role_id AND r2.deleted = 0 AND r2.status = 1
                WHERE ur2.user_id = u.id AND r2.role_code = 'support'
            ) DESC, u.id
            LIMIT 50
            </script>
            """)
    List<UserSnapshot> selectSupportUsers(@Param("keyword") String keyword);
}
