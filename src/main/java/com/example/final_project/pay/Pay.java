package com.example.final_project.pay;

import com.example.final_project.reservation.Reservation;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Table(name = "pay_tb")
@Entity
public class Pay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 결제 번호

    @OneToOne(fetch = FetchType.LAZY, optional = false) // optional = false를 적어야 Not Null이 된다.
    private Reservation reservation; // 예약 번호

    @Column(nullable = false)
    private Integer amount; // 결제 금액

    @Column(nullable = false)
    private String way; // 결제 방식

    @Column(nullable = false)
    private String state; // 결제 유무 (ex. Credit Card, Debit Card, Bank Transfer, Mobile Payment ...)

    @CreationTimestamp
    private LocalDateTime createdAt; // 결제 일자

    @Builder
    public Pay(Integer id, Reservation reservation, Integer amount, String way, String state, LocalDateTime createdAt) {
        this.id = id;
        this.reservation = reservation;
        this.amount = amount;
        this.way = way;
        this.state = state;
        this.createdAt = createdAt;
    }
}