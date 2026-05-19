package io.spring.infrastructure.mybatis.mapper;

import io.spring.core.user.FollowRelation;
import io.spring.core.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
  void upsert(@Param("user") User user);

  User findByUsername(@Param("username") String username);

  User findByEmail(@Param("email") String email);

  User findById(@Param("id") String id);

  FollowRelation findRelation(@Param("userId") String userId, @Param("targetId") String targetId);

  void saveRelation(@Param("followRelation") FollowRelation followRelation);

  void deleteRelation(@Param("followRelation") FollowRelation followRelation);
}
