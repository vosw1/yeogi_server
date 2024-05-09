package com.example.final_project.stay;

import com.example.final_project._core.enums.RoomEnum;
import com.example.final_project._core.errors.exception.Exception400;
import com.example.final_project._core.errors.exception.Exception401;
import com.example.final_project._core.errors.exception.Exception403;
import com.example.final_project._core.errors.exception.Exception404;
import com.example.final_project.company.Company;
import com.example.final_project.company.CompanyRepository;
import com.example.final_project.company.SessionCompany;
import com.example.final_project.option.Option;
import com.example.final_project.option.OptionRepository;
import com.example.final_project.stay_image.StayImage;
import com.example.final_project.stay_image.StayImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StayService {
    private final StayRepository stayRepository;
    private final CompanyRepository companyRepository;
    private final OptionRepository optionRepository;
    private final StayImageRepository stayImageRepository;

    @Transactional
    public void register(StayRequest.SaveDTO reqDTO, SessionCompany sessionUser) {

        //1. 인증처리
        Optional<Company> companyOP = companyRepository.findById(sessionUser.getId());
        Company company = companyOP.orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다"));

        //2. 권한처리
        if (!company.getId().equals(sessionUser.getId())) {
            throw new Exception401("숙소를 등록할 권한이 없습니다.");
        }


        Stay stay = stayRepository.save(reqDTO.toEntity(company));

        // 옵션 등록
        if (reqDTO.getOptions() != null && !reqDTO.getOptions().isEmpty()) {
            List<Option> options = reqDTO.getOptions().stream()
                    .map(optionName -> {
                        return new Option(stay, optionName);
                    })
                    .toList();

            optionRepository.saveAll(options);
        }

        //사진 등록
        StayImage stayImage = new StayImage(stay);
        stayImageRepository.save(stayImage);
    }

    //숙소 등록폼
    @Transactional
    public StayResponse.UpdateFormDTO updateForm(Integer stayId, SessionCompany sessionUser) {
        // 1. 인증 처리
        if (sessionUser == null) {
            throw new Exception400("로그인이 필요한 서비스입니다");
        }

        Stay stay = stayRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다."));

        Company company = companyRepository.findByStayId(stay.getId())
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 없습니다"));

        // 2. 권한 처리
        if (!sessionUser.getId().equals(company.getId())) {
            throw new Exception401("정보를 수정할 권한이 없습니다");
        }

        List<Option> options = optionRepository.findByStayId(stay.getId());

        // Option을 OptionChekedDTO로 변환
        List<StayResponse.UpdateFormDTO.OptionChekedDTO> optionDTOs = new ArrayList<>();
        optionDTOs.add(new StayResponse.UpdateFormDTO.OptionChekedDTO(options));
        return new StayResponse.UpdateFormDTO(stay, optionDTOs);

    }

    //숙소 수정
    @Transactional
    public void update(Integer stayId, SessionCompany sessionCompany, StayRequest.UpdateDTO reqDTO) {

        //1. 인증처리
        Stay stay = stayRepository.findById(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다."));

        //2. 권한처리
        if (stay.getCompany().getId() != sessionCompany.getId()) {
            throw new Exception403("해당 숙소를 수정할 권한이 없습니다.");
        }

        //3. 숙소정보 저장
        stay.updateStay(reqDTO);

        List<Option> beforeOptions = optionRepository.findByStayId(stayId);

        //4. 옵션 삭제
        beforeOptions.clear();
        optionRepository.deleteBystayId(stayId);

        //5. 옵션 저장
        if (reqDTO.getOptions() != null && !reqDTO.getOptions().isEmpty()) {
            List<Option> options = reqDTO.getOptions().stream()
                    .map(optionName -> {
                        return new Option(stay, optionName);
                    })
                    .toList();

            optionRepository.saveAll(options);

        }

    }

    //숙소 삭제
    @Transactional
    public StayResponse.Delete delete(Integer stayId, SessionCompany sessionCompany) {
        //1. 인증처리

        if (sessionCompany.getId() == null) {
            throw new Exception401("로그인이 필요한 서비스입니다.");
        }

        Stay stay = stayRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 숙소를 찾을 수 없습니다."));

        //2. 권한처리
        Company company = companyRepository.findByStayId(stayId)
                .orElseThrow(() -> new Exception404("해당 기업을 찾을 수 업습니다."));

        if (sessionCompany.getId() != company.getId()) {
            throw new Exception403("삭제할 권한이 없습니다");
        }

        //3. 삭제(state 업데이트)
        stay.deleteStay(stay.getState());

        return new StayResponse.Delete(stay);
    }

//    // 숙소 검색 기능 (이름, 지역, 날짜, 가격, 인원 수, 예약 날짜 별 검색) // request 방식
//    public List<StayResponse.SearchListDTO> getSearchStayList(StayRequest.SearchDTO reqDTO) {
//        List<StayResponse.SearchListDTO> resultList;
//
//        resultList = stayRepository.findBySearchStay(reqDTO.getName(), reqDTO.getAddress(), reqDTO.getPrice(), reqDTO.getPerson(), reqDTO.getCheckInDate(), reqDTO.getCheckOutDate()).stream()
//                .map(StayResponse.SearchListDTO::new)
//                .toList();
//
//        return resultList;
//    }

    // 숙소 검색 기능 (이름, 지역, 날짜, 가격, 인원 수 검색)
    public List<StayResponse.SearchListDTO> getSearchStayList(
            String stayName,
            String stayAddress,
            Integer roomPrice,
            Integer person
    ) {
        List<Stay> stayList = stayRepository.findBySearchStay(stayName, stayAddress, roomPrice, person);
        return stayList.stream()
                .map(StayResponse.SearchListDTO::new)
                .toList();
    }

    // 특가숙소
    public List<StayResponse.SpecialpriceList> findSpecialListByRoom() {
        RoomEnum state = RoomEnum.APPLIED;
        System.out.println(3);
        // 특정 상태에 해당하는 숙소 리스트 조회
        List<Stay> specialList = stayRepository.findStayBySpecial(state);
        System.out.println(4);
        // 조회된 숙소 리스트가 null이면 빈 리스트로 초기화
        if (specialList == null) {
            specialList = Collections.emptyList();
        }
        System.out.println(5);
        // 숙소 리스트를 매핑하여 결과 리스트 생성
        List<StayResponse.SpecialpriceList> resultList = specialList.stream()
                .map(stay -> {
                    // 각 숙소에 대한 이미지 조회
                    StayImage stayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    // SpecialpriceList 객체 생성
                    return new StayResponse.SpecialpriceList(stay, stayImage);
                })
                .collect(Collectors.toList());

        // 결과 리스트가 null이면 빈 리스트로 초기화
        if (resultList == null) {
            resultList = Collections.emptyList();
        }
        System.out.println(6);
        // 결과 리스트 반환
        return resultList;
    }



    @Transactional
    public StayResponse.AllList findAllStayWithCategory(){

        // 국내 숙소 찾기
        List<Stay> domesticStays = stayRepository.findAll().stream()
                .filter(stay -> !stay.getCategory().equals("해외"))
                .collect(Collectors.toList());

        System.out.println("국내결과===========================================" + domesticStays.size());

        List<StayResponse.AllList.DomesticDTO> domesticDTOs = domesticStays.stream()
                .map(stay -> {
                    StayImage domesticStayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    String imageName = (domesticStayImage != null) ? domesticStayImage.getName() : null;
                    String imagePath = (domesticStayImage != null) ? domesticStayImage.getPath() : null;
                    return new StayResponse.AllList.DomesticDTO(stay, imageName, imagePath);
                })
                .collect(Collectors.toList());


        // 해외 숙소 찾기
        List<Stay> overseaStays = stayRepository.findAll().stream()
                .filter(stay -> stay.getCategory().equals("해외"))
                .collect(Collectors.toList());

        System.out.println("해외결과===========================================" +overseaStays.size());


        List<StayResponse.AllList.OverseaDTO> overseaDTOs = overseaStays.stream()
                .map(stay -> {
                    StayImage overseaStayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    return new StayResponse.AllList.OverseaDTO(stay, overseaStayImage);
                })
                .collect(Collectors.toList());

        // 특가 숙소 찾기
        List<Stay> specialPriceStays = stayRepository.findAll().stream()
                .filter(stay -> stay.getRooms().stream().anyMatch(room -> room.getSpecialState() == RoomEnum.APPLIED))
                .collect(Collectors.toList());

        System.out.println("특가결과===========================================" +specialPriceStays.size());


        List<StayResponse.AllList.SpecialPriceDTO> specialPriceDTOs = specialPriceStays.stream()
                .map(stay -> {
                    StayImage specialPriceStayImage = stayImageRepository.findByStayId(stay.getId()).stream().findFirst().orElse(null);
                    return new StayResponse.AllList.SpecialPriceDTO(stay, specialPriceStayImage);
                })
                .collect(Collectors.toList());

        return new StayResponse.AllList(specialPriceDTOs, domesticDTOs, overseaDTOs);
    }










}
