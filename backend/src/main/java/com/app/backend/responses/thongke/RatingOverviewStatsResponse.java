package com.app.backend.responses.thongke;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RatingOverviewStatsResponse {

    @JsonProperty("tong_danh_gia_bo_cau_hoi")
    private Long tongDanhGiaBoCauHoi;

    @JsonProperty("tong_danh_gia_khoa_hoc")
    private Long tongDanhGiaKhoaHoc;

    @JsonProperty("trung_binh_sao_bo_cau_hoi")
    private Double trungBinhSaoBoCauHoi;

    @JsonProperty("trung_binh_sao_khoa_hoc")
    private Double trungBinhSaoKhoaHoc;

    @JsonProperty("top_rated_bo_cau_hoi")
    private List<RatingStatsItemResponse> topRatedBoCauHoi;

    @JsonProperty("top_rated_khoa_hoc")
    private List<RatingStatsItemResponse> topRatedKhoaHoc;

    @JsonProperty("lowest_rated_bo_cau_hoi")
    private List<RatingStatsItemResponse> lowestRatedBoCauHoi;

    @JsonProperty("lowest_rated_khoa_hoc")
    private List<RatingStatsItemResponse> lowestRatedKhoaHoc;

    @JsonProperty("most_reviewed_bo_cau_hoi")
    private List<RatingStatsItemResponse> mostReviewedBoCauHoi;

    @JsonProperty("most_reviewed_khoa_hoc")
    private List<RatingStatsItemResponse> mostReviewedKhoaHoc;
}
