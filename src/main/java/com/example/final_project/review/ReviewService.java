package com.example.final_project.review;

import com.example.final_project._core.errors.exception.Exception400;
import com.example.final_project._core.errors.exception.Exception401;
import com.example.final_project._core.errors.exception.Exception404;
import com.example.final_project.company.Company;
import com.example.final_project.company.CompanyRepository;
import com.example.final_project.company.SessionCompany;
import com.example.final_project.report.Report;
import com.example.final_project.report.ReportRepository;
import com.example.final_project.reservation.Reservation;
import com.example.final_project.reservation.ReservationRepository;
import com.example.final_project.room.RoomRepository;
import com.example.final_project.stay.Stay;
import com.example.final_project.stay.StayRepository;
import com.example.final_project.user.SessionUser;
import com.example.final_project.user.User;
import com.example.final_project.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final StayRepository stayRepository;
    private final CompanyRepository companyRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ReportRepository reportRepository;


    //댓글 작성
    @Transactional
    public ReviewResponse.Save insert(Integer stayId,ReviewRequest.ReviewRequestDTO reqDTO, Object sessionObject) {

        // 1. 인증 처리
        if (sessionObject == null) {
            throw new Exception400("로그인이 필요한 서비스입니다.");
        }

        Stay stay = stayRepository.findById(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다"));

        Review review = reqDTO.toEntity(sessionObject, stay);

        // 3. 부모댓글 처리
        Review parentReview;

        if (reqDTO.getParentId() != null) {
            parentReview = reviewRepository.findById(reqDTO.getParentId())
                    .orElseThrow(() -> new Exception404("해당 리뷰아이디를 찾을 수 없습니다. : " + reqDTO.getParentId()));
            review.updateParent(parentReview);
        }

        review.updateBoard(stay);

        reviewRepository.save(review);

        ReviewResponse.Save.UserDTO writerDTO = null;
        if (review.getUser() != null){
            User user =userRepository.findById(reqDTO.getUserId()).orElseThrow(() -> new Exception404("해당 유저를 찾지 못했습니다"));
            writerDTO = new ReviewResponse.Save.UserDTO(user);
            Reservation reservation = reservationRepository.findByStayIdWithUserId(reqDTO.getStayId(),reqDTO.getUserId(),reqDTO.getRoomId());
            reservation.setReviewid(review.getId());
        } else if (review.getCompany() != null){

        }

        return new ReviewResponse.Save(stayId,writerDTO,review.getContent(),review.getScore());

    }


    // 댓글 목록
    @Transactional
    public List<ReviewResponse.Find> select(Integer stayId , SessionCompany sessionUser) {

        // 1. 인증 처리
        if (sessionUser == null){
            new Exception401("로그인이 필요한 서비스입니다.");
        }

        Stay stay = stayRepository.findById(stayId)
                .orElseThrow(() -> new Exception401("조회할 권한이 없습니다"));

        Company company = companyRepository.findByStayId(sessionUser.getId())
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다"));

        // 2. 권한 처리
        if (sessionUser.getId() != company.getId()){
            new Exception400("해당 기업의 리뷰를 조회 할 권한이 없습니다.");
        }

        // 3. 리스트 조회
        List<Review> reviewList = reviewRepository.findAllByStayIdWithDetails(stayId);

        // 4. parent가 없는 리뷰만 담도록 가공 parent가 없으면 대댓글이 아니다.

        List<Review> filteredReviewList = reviewList.stream()
                .filter(review -> review.getParent() == null)
                .collect(Collectors.toList());

        Integer reviewCount = filteredReviewList.size();

        // ReviewResponse.Find 객체로 변환하여 반환
        Map<Integer, ReviewResponse.Find> reviewMap = new HashMap<>();
        List<ReviewResponse.Find> roots = new ArrayList<>();

        for (Review review : reviewList) {
            ReviewResponse.Find reviewFind = new ReviewResponse.Find(
                    review.getId(),
                    new ReviewResponse.Find.UserDTO(review.getUser()),
                    review.getContent(),
                    review.getCreatedAt(),
                    review.getScore(),
                    reviewCount
            );

            reviewMap.put(reviewFind.getId(), reviewFind);

            if (review.getParent() != null) {
                ReviewResponse.Find parentFind = reviewMap.get(review.getParent().getId());
                if (parentFind != null) {
                    parentFind.getChildren().add(reviewFind);
                }
            } else {
                roots.add(reviewFind);
            }
        }
        return roots;

    }

    // 댓글 디테일
    @Transactional
    public ReviewResponse.Detail detail(Integer reviewId, SessionCompany sessionUser) {
        // 1. 인증 처리
        if (sessionUser == null) {
            throw new Exception401("로그인이 필요한 서비스입니다.");
        }

        // 2. 리뷰 조회 및 지연 로딩 초기화
        Review review = reviewRepository.findByReviewId(reviewId);

        // 3. 리뷰 작성자 정보 생성
        ReviewResponse.Detail.UserDTO writerDTO = null;
        if (review.getUser() != null) {
            writerDTO = new ReviewResponse.Detail.UserDTO(review.getUser());
        } else if (review.getCompany() != null) {
            writerDTO = new ReviewResponse.Detail.UserDTO(review.getCompany());
        }

        // 4. 리뷰 디테일 정보 생성
        ReviewResponse.Detail detail = new ReviewResponse.Detail(review, writerDTO);

        // 5. 자식 리뷰 리스트 구성
        for (Review childReview : review.getChildren()) {
            Hibernate.initialize(childReview.getStay().getOptions()); // 자식 리뷰의 options 컬렉션 초기화
            ReviewResponse.Detail.UserDTO childWriterDTO = null;
            if (childReview.getUser() != null) { // 유저 비었을떄
                childWriterDTO = new ReviewResponse.Detail.UserDTO(childReview.getUser());
            } else if (childReview.getCompany() != null) { // 기업 비었을때
                childWriterDTO = new ReviewResponse.Detail.UserDTO(childReview.getCompany());
            }

            ReviewResponse.Detail childDetail = new ReviewResponse.Detail(childReview, childWriterDTO);
            detail.getChildren().add(childDetail);
        }

        return detail;
    }

    //댓글 신고폼
    @Transactional
    public ReviewResponse.ReportForm reportForm(Integer reviewId,SessionCompany sessionUser){
        // 1. 인증 처리
        if (sessionUser == null) {
            throw new Exception401("로그인이 필요한 서비스입니다.");
        }

        // 2. 댓글 조회
        Review review = reviewRepository.findByReviewId(reviewId);

        return new ReviewResponse.ReportForm(review);
    }

    //댓글 신고
    @Transactional
    public void reportSave(Integer reviewId, SessionCompany sessionUser, ReviewRequest.ReportRequestDTO reqDTO){

        // 1. 인증 처리
        if (sessionUser == null) {
            throw new Exception401("로그인이 필요한 서비스입니다.");
        }

        // 2. 댓글 조회
        Review review = reviewRepository.findByReviewId(reviewId);

        // 3. 유저 찾기
        User user = userRepository.findById(review.getUser().getId())
                .orElseThrow(() -> new Exception404("해당 유저를 찾을 수 없습니다"));

        Report report = reqDTO.toEntity(user,review);

        reportRepository.save(report);
        review.setStateReported(review.getState()); // 리뷰의 상태값을 REPORTED로 변경

    }


    //댓글 삭제
    @Transactional
    public ReviewResponse.Delete delete(Integer reviewId){

        // 댓글찾기
        Review review = reviewRepository.findReviewByIdWithParent(reviewId);

        //삭제
        review.setStateDeleted(review.getState());

        return new ReviewResponse.Delete(review.getState());
    }

    //유저가 적은 리뷰리스트
    @Transactional
    public List<ReviewResponse.ReveiwListDTO> reviewList(Integer userId , SessionUser sessionUser){

        // 1.인증 처리
        if (sessionUser == null){
            throw new Exception400("로그인이 필요한 서비스입니다");
        }

        // 2.권한 처리
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new Exception404("해당 유저를 찾을 수 없습니다"));

        if (user.getId() != sessionUser.getId()){
            throw new Exception401("리뷰를 확인할 권한이 없습니다");
        }

        List<Review> reviewList = reviewRepository.findByUserIdWithUserAndRoom(user.getId());

        List<ReviewResponse.ReveiwListDTO> resultList = reviewList.stream()
                .map(
                        ReviewResponse.ReveiwListDTO::new
                ).toList();

        return  resultList;
    }

}