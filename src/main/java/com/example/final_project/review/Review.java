package com.example.final_project.review;

import com.example.final_project._core.enums.ReviewEnum;
import com.example.final_project.room.Room;
import com.example.final_project.stay.Stay;
import com.example.final_project.user.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@NoArgsConstructor
@Data
@Table(name = "review_tb")
@Entity
@JsonIgnoreProperties({"user", "room"})
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 리뷰 번호

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // optional = false를 적어야 Not Null이 된다.
    @JoinColumn(name = "user_id")
    private User writer; // 리뷰 남긴 유저 번호

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stay_id")
    private Stay stay; // 리뷰한 객실 번호

    @Column(nullable = false)
    private Integer score; // 평점

    @Column(nullable = false)
    private String content; // 내용

    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    private ReviewEnum isDelete; // 삭제 여부(FLAWLESS : 문제 없는 댓글, COMPLETE: 삭제 됨, FAIL: 삭제 안 됨)

    @CreationTimestamp
    private LocalDateTime createdAt; // 리뷰 작성 날짜

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Review parent;

    @Builder
    public Review(Integer id, User user, Stay stay, Integer score, String content, ReviewEnum isDelete, LocalDateTime createdAt) {

        this.id = id;
        this.writer = user;
        this.stay = stay;
        this.score = score;
        this.content = content;
        this.isDelete = isDelete;
        this.createdAt = createdAt;
    }

    public Review(String content) {
        this.content = content;
    }

    public void updateWriter(User user) {
        this.writer = user;
    }

    public void updateBoard(Stay stay) {
        this.stay = stay;
    }

    public void updateParent(Review review) {
        this.parent = review;
    }

    public void changeIsDeleted(ReviewEnum isDelete) {
        this.isDelete = isDelete;
    }
}