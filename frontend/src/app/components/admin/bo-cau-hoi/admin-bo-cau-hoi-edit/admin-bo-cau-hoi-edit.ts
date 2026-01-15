import { CommonModule, NgClass } from '@angular/common';
import { Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import Swal from 'sweetalert2';
import { ChuDe } from '../../../../models/chude';
import { BoCauHoiResponse } from '../../../../responses/bocauhoi/bocauhoi-response';
import { ResponseObject } from '../../../../responses/response-object';
import { Base } from '../../../base/base';

@Component({
  selector: 'app-admin-bo-cau-hoi-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, NgClass],
  templateUrl: './admin-bo-cau-hoi-edit.html',
  styleUrl: './admin-bo-cau-hoi-edit.scss',
})
export class AdminBoCauHoiEdit extends Base implements OnInit {
  @ViewChild('editForm') editForm!: NgForm;

  bo_cau_hoi_id!: number;
  dto: any = {
    tieu_de: '',
    mo_ta: '',
    chu_de_id: 0,
    che_do_hien_thi: 'RIENG_TU',
    loai_su_dung: 'CHI_XEP_HANG',
    muon_tao_tra_phi: false,
  };

  loading = false;
  saving = false;
  chu_des: ChuDe[] = [];

  readonly loaiSuDungOptions = [
    { value: 'CHI_XEP_HANG', label: 'Chỉ xếp hạng (Thi đấu xếp hạng)', icon: 'fa-trophy' },
    { value: 'CHI_THUONG', label: 'Chỉ thường (Đấu vui)', icon: 'fa-gamepad' },
    { value: 'CHI_LUYEN_TAP', label: 'Chỉ luyện tập (Luyện tập)', icon: 'fa-book' },
    {
      value: 'CHI_KHOA_HOC',
      label: 'Chỉ khóa học (Chỉ dùng cho khóa học)',
      icon: 'fa-graduation-cap',
    },
  ];

  showChuDeDropdown = false;
  showCheDoDropdown = false;

  ngOnInit(): void {
    this.bo_cau_hoi_id = Number(this.route.snapshot.paramMap.get('id'));
    this.fetchChuDes();
    this.fetchBoCauHoi();
  }

  fetchChuDes(): void {
    this.chuDeService.getChuDe(0, 100).subscribe({
      next: (res: ResponseObject<any>) => {
        this.chu_des = res.data || [];
      },
      error: () => {
        Swal.fire('Lỗi', 'Không thể tải danh sách chủ đề', 'error').then((r) => {});
      },
    });
  }

  fetchBoCauHoi(): void {
    this.loading = true;
    this.bocauHoiService.getById(this.bo_cau_hoi_id).subscribe({
      next: (res: ResponseObject<BoCauHoiResponse>) => {
        const d = res.data!;
        this.dto = {
          tieu_de: d.tieu_de,
          mo_ta: d.mo_ta,
          chu_de_id: d.chu_de_id,
          che_do_hien_thi: d.che_do_hien_thi,
          loai_su_dung: d.loai_su_dung || 'CHI_XEP_HANG',
          muon_tao_tra_phi: d.muon_tao_tra_phi || false,
        };
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        Swal.fire('Lỗi', 'Không thể tải dữ liệu bộ câu hỏi', 'error').then(() => {
          this.router.navigate(['/admin/bo-cau-hoi']).then();
        });
      },
    });
  }

  onSubmit(form: NgForm): void {
    if (form.invalid) {
      Swal.fire('Cảnh báo', 'Vui lòng nhập đầy đủ thông tin', 'warning').then((r) => {});
      return;
    }

    this.saving = true;
    this.bocauHoiService.update(this.bo_cau_hoi_id, this.dto).subscribe({
      next: () => {
        Swal.fire('Thành công', 'Cập nhật bộ câu hỏi thành công', 'success').then(() => {
          this.router.navigate(['/admin/bo-cau-hoi', this.bo_cau_hoi_id]).then();
        });
      },
      error: (err) => {
        Swal.fire('Lỗi', err.error?.message || 'Không thể cập nhật bộ câu hỏi', 'error').then(
          (r) => {}
        );
      },
      complete: () => (this.saving = false),
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/bo-cau-hoi', this.bo_cau_hoi_id]).then();
  }

  getIconColor(type: string): string {
    switch (type) {
      case 'CHI_XEP_HANG':
        return 'purple';
      case 'CHI_THUONG':
        return 'blue';
      case 'CHI_LUYEN_TAP':
        return 'green';
      case 'CHI_KHOA_HOC':
        return 'orange';
      default:
        return 'blue';
    }
  }

  getUsageDesc(type: string): string {
    switch (type) {
      case 'CHI_XEP_HANG':
        return 'Dùng cho thi đấu xếp hạng chính thức (Official).';
      case 'CHI_THUONG':
        return 'Dùng cho đấu giải trí, giao hữu không tính điểm.';
      case 'CHI_LUYEN_TAP':
        return 'Chỉ dùng để luyện tập cá nhân.';
      case 'CHI_KHOA_HOC':
        return 'Dành riêng cho bài kiểm tra trong khóa học.';
      default:
        return '';
    }
  }

  toggleChuDeDropdown() {
    this.showChuDeDropdown = !this.showChuDeDropdown;
    this.showCheDoDropdown = false; // Đóng cái kia nếu đang mở
  }

  selectChuDe(id: number) {
    this.dto.chu_de_id = id;
    this.showChuDeDropdown = false;
  }

  getSelectedChuDeName(): string {
    if (!this.dto.chu_de_id) return '-- Chọn chủ đề --';
    const selected = this.chu_des.find(c => c.id === this.dto.chu_de_id);
    return selected ? selected.ten : '-- Chọn chủ đề --';
  }

  toggleCheDoDropdown() {
    this.showCheDoDropdown = !this.showCheDoDropdown;
    this.showChuDeDropdown = false;
  }

  selectCheDo(val: string) {
    this.dto.che_do_hien_thi = val;
    this.showCheDoDropdown = false;
  }

  getSelectedCheDoName(): string {
    if (this.dto.che_do_hien_thi === 'RIENG_TU') return '🔒 Private (Dùng để tổ chức thi đấu)';
    if (this.dto.che_do_hien_thi === 'CONG_KHAI') return '🌍 Public (Công khai cho mọi người)';
    return '-- Chọn chế độ --';
  }

  closeAllDropdowns() {
    this.showChuDeDropdown = false;
    this.showCheDoDropdown = false;
  }
}
