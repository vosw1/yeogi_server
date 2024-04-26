package com.example.final_project.stay;

import com.example.final_project.company.Company;
import com.example.final_project.option.Option;
import com.example.final_project.room.Room;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@Data
@Table(name = "stay_tb")
@Entity
public class Stay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 숙소 번호

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company; // 해당 숙소의 기업

    @Column(nullable = false)
    private String name; // 숙소 이름

    @Column(nullable = false)
    private String category; // 숙소 분류

    @Column(nullable = false)
    private String address; // 숙소 주소

    @Column(nullable = false)
    private String intro; // 숙소 소개

    @Column(nullable = false)
    private String information; // 숙소 이용 정보

    private String imageName; // 이미지 파일명

    private String imagePath; // 이미지 경로명

    @OneToMany(mappedBy = "stay", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Option> options = new ArrayList<>(); // 옵션 리스트

    @OneToMany(mappedBy = "stay", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private List<Room> rooms = new ArrayList<>(); // 방(객실) 옵션 리스트

    @CreationTimestamp
    private LocalDateTime createdAt; // 숙소 등록 일자

    @Builder
    public Stay(Integer id, Company company, String name, String category, String address, String intro, String information, String imageName, String imagePath, List<Option> options, LocalDateTime createdAt) {
        this.id = id;
        this.company = company;
        this.name = name;
        this.category = category;
        this.address = address;
        this.intro = intro;
        this.information = information;
        this.imageName = imageName;
        this.imagePath = imagePath;
        this.options = options;
        this.createdAt = createdAt;
    }


    public void upDateStay(StayRequest.UpdateDTO reqDTO){
        this.intro = reqDTO.getIntro();
        this.information = reqDTO.getInformation();
        this.imageName = reqDTO.getImageName();
        this.imagePath = reqDTO.getImagePath();

        // Option 리스트를 업데이트
        List<Option> updatedOptions = new ArrayList<>();
        Map<Integer, Option> optionMap = reqDTO.getOptionList().stream()
                .collect(Collectors.toMap(Option::getId, option -> option));
        for (Option option : this.options) {

            if (optionMap.containsKey(option.getId())) {
                updatedOptions.add(optionMap.get(option.getId()));
            }
        }
        this.options = updatedOptions;
    }
}