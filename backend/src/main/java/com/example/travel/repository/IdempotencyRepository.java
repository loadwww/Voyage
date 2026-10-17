package com.example.travel.repository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 幂等键存储（写操作防重放）。
 * 客户端在 POST 请求头携带 Idempotency-Key，同一键的重复请求直接返回首次结果。
 */
@Repository
public class IdempotencyRepository {
    private final JdbcTemplate jdbcTemplate;

    public IdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return 该幂等键首次请求对应的资源 id；不存在返回 null
     */
    public Long findReference(String idempotencyKey) {
        List<Long> result = jdbcTemplate.query(
                "SELECT response_ref FROM idempotency_record WHERE idempotency_key = ?",
                (rs, rowNum) -> rs.getLong("response_ref"),
                idempotencyKey
        );
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 保存幂等键。并发下只有一个请求插入成功，其余抛 {@link DuplicateKeyException}。
     */
    public void save(String idempotencyKey, long referenceId) {
        jdbcTemplate.update(
                "INSERT INTO idempotency_record (idempotency_key, response_ref) VALUES (?, ?)",
                idempotencyKey, referenceId
        );
    }
}
