package com.example.final_project.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Integer> {

    // 로그인 인증 쿼리
    @Query("select c from Company c where c.email = :email and c.password = :password")
    Optional<Company> findByIdAndPassword(@Param("email") String email, @Param("password") String password);

    // StayId로 Company찾기쿼리
    @Query("select c from Company c join fetch Stay s on c.id = s.company.id  where c.id = :stayId ")
    Optional<Company> findByStayId(@Param("stayId") Integer stayId);
}
