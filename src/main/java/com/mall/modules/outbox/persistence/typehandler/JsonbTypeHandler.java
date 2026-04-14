package com.mall.modules.outbox.persistence.typehandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL jsonb 字段专用类型处理器。
 *
 * <p>MyBatis-Plus 自带的 JacksonTypeHandler 更偏向普通 JSON 字符串场景，
 * 直接写 PostgreSQL jsonb 时容易被数据库识别成 varchar。
 * 这里显式包装成 PGobject(type=jsonb)，保证插入和查询都走原生 jsonb。</p>
 */
public class JsonbTypeHandler extends BaseTypeHandler<JsonNode> {

	/** 统一复用一个带默认模块的 ObjectMapper，保证时间等常见类型也能被序列化。 */
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

	/**
	 * 把 JsonNode 按 PostgreSQL jsonb 参数写入 PreparedStatement。
	 */
	@Override
	public void setNonNullParameter(
		PreparedStatement preparedStatement,
		int index,
		JsonNode parameter,
		JdbcType jdbcType
	) throws SQLException {
		PGobject jsonObject = new PGobject();
		jsonObject.setType("jsonb");
		jsonObject.setValue(parameter.toString());
		preparedStatement.setObject(index, jsonObject);
	}

	/**
	 * 从查询结果中读取 jsonb 字段，并还原成 JsonNode。
	 */
	@Override
	public JsonNode getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
		return readJsonNode(resultSet.getString(columnName));
	}

	/**
	 * 从查询结果中读取 jsonb 字段，并还原成 JsonNode。
	 */
	@Override
	public JsonNode getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
		return readJsonNode(resultSet.getString(columnIndex));
	}

	/**
	 * 从存储过程结果中读取 jsonb 字段，并还原成 JsonNode。
	 */
	@Override
	public JsonNode getNullableResult(CallableStatement callableStatement, int columnIndex) throws SQLException {
		return readJsonNode(callableStatement.getString(columnIndex));
	}

	/**
	 * 把数据库里的 JSON 文本解析成 JsonNode。
	 */
	private JsonNode readJsonNode(String json) throws SQLException {
		if (json == null || json.isBlank()) {
			return null;
		}

		try {
			return OBJECT_MAPPER.readTree(json);
		} catch (Exception exception) {
			throw new SQLException("Failed to parse jsonb column to JsonNode", exception);
		}
	}
}
