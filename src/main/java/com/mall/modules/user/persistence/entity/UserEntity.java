package com.mall.modules.user.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mall.modules.user.domain.UserRole;
import com.mall.modules.user.domain.UserStatus;

import java.time.Instant;

/**
 * 用户表实体，对应数据库中的 {@code users} 表。
 */
@TableName("users")
public class UserEntity {

	/** 用户主键。 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/** 登录用户名。 */
	private String username;

	/** 展示昵称。 */
	private String nickname;

	/** 加密后的密码摘要。 */
	private String passwordHash;

	/** 用户角色。 */
	private UserRole role;

	/** 账号状态。 */
	private UserStatus status;

	/** 创建时间。 */
	@TableField(fill = FieldFill.INSERT)
	private Instant createdAt;

	/** 最近更新时间。 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private Instant updatedAt;

	/**
	 * 返回用户主键。
	 */
	public Long getId() {
		return id;
	}

	/**
	 * 返回登录用户名。
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * 设置登录用户名。
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 返回展示昵称。
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * 设置展示昵称。
	 */
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	/**
	 * 返回加密后的密码摘要。
	 */
	public String getPasswordHash() {
		return passwordHash;
	}

	/**
	 * 设置加密后的密码摘要。
	 */
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/**
	 * 返回用户角色。
	 */
	public UserRole getRole() {
		return role;
	}

	/**
	 * 设置用户角色。
	 */
	public void setRole(UserRole role) {
		this.role = role;
	}

	/**
	 * 返回当前账号状态。
	 */
	public UserStatus getStatus() {
		return status;
	}

	/**
	 * 设置当前账号状态。
	 */
	public void setStatus(UserStatus status) {
		this.status = status;
	}

	/**
	 * 返回创建时间。
	 */
	public Instant getCreatedAt() {
		return createdAt;
	}

	/**
	 * 返回最近更新时间。
	 */
	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
