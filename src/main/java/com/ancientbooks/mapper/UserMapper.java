package com.ancientbooks.mapper;

import com.ancientbooks.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（自定义SQL，避免Wrapper兼容性问题）
     */
    @Select("SELECT id, username, password, email, role, status, create_time, update_time, deleted " +
            "FROM user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    User findByUsername(String username);
}
