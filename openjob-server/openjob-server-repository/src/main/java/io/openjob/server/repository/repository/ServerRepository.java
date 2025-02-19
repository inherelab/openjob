package io.openjob.server.repository.repository;

import io.openjob.server.repository.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * @author stelin <swoft@qq.com>
 * @since 1.0.0
 */
public interface ServerRepository extends JpaRepository<Server, Long> {
    @Query(value = "update Server as s set s.status=?2,s.updateTime=?3 where s.id=?1")
    Integer update(Long id, Integer status, Integer updateTime);
}
