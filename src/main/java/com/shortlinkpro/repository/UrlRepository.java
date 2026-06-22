package com.shortlinkpro.repository;

import com.shortlinkpro.entity.Url;
import com.shortlinkpro.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    Optional<Url> findByShortCode(String shortCode);
    Optional<Url> findByCustomAlias(String customAlias);
    Page<Url> findByUser(User user, Pageable pageable);
    
    @Query("SELECT u FROM Url u WHERE u.user = :user AND " +
           "(LOWER(u.originalUrl) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.shortCode) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.customAlias) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Url> searchUrls(@Param("user") User user, @Param("query") String query, Pageable pageable);

    long countByUser(User user);
    long countByUserAndStatus(User user, String status);

    @Query("SELECT SUM(u.clicks) FROM Url u WHERE u.user = :user")
    Long sumClicksByUser(@Param("user") User user);
}
