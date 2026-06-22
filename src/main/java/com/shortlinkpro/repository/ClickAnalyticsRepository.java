package com.shortlinkpro.repository;

import com.shortlinkpro.entity.ClickAnalytics;
import com.shortlinkpro.entity.Url;
import com.shortlinkpro.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {
    List<ClickAnalytics> findByUrlOrderByClickTimeDesc(Url url);
    
    @Query("SELECT ca FROM ClickAnalytics ca WHERE ca.url.user = :user ORDER BY ca.clickTime DESC")
    List<ClickAnalytics> findRecentClicksByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT ca FROM ClickAnalytics ca WHERE ca.url.user = :user")
    List<ClickAnalytics> findAllByUser(@Param("user") User user);
    
    @Query("SELECT ca FROM ClickAnalytics ca WHERE ca.url = :url")
    List<ClickAnalytics> findAllByUrl(@Param("url") Url url);
}
