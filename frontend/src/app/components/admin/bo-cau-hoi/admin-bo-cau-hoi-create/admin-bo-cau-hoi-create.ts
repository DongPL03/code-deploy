import { NgClass } from '@angular/common';
import { Component, OnInit, ViewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import Swal from 'sweetalert2';
import { ChuDe } from '../../../../models/chude';
import { ResponseObject } from '../../../../responses/response-object';
import { Base } from '../../../base/base';

@Component({
  selector: 'app-admin-bo-cau-hoi-create',
  imports: [FormsModule, NgClass],
  standalone: true,
  templateUrl: './admin-bo-cau-hoi-create.html',
  styleUrl: './admin-bo-cau-hoi-create.scss',
})
export class AdminBoCauHoiCreate extends Base implements OnInit {
  @ViewChild('createForm') createForm!: NgForm;

  loading = false;

  chu_de_list: ChuDe[] = [];

  form: any = {
    tieu_de: '',
    mo_ta: '',
    chu_de_id: 0,
    che_do_hien_thi: 'RIENG_TU', // gợi ý: admin tạo bộ thi đấu → để RIENG_TU
    loai_su_dung: 'CHI_XEP_HANG', // Mặc định cho admin: CHI_XEP_HANG
    muon_tao_tra_phi: false, // Admin có thể tạo trả phí hoặc miễn phí
  };

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
    this.loadChuDe();
  }

  loadChuDe(): void {
    this.chuDeService.getChuDe(0, 100).subscribe({
      next: (res: ResponseObject<ChuDe[]>) => {
        this.chu_de_list = res.data || [];
      },
      error: () => {
        Swal.fire('Lỗi', 'Không thể tải danh sách chủ đề', 'error').then((r) => {});
      },
    });
  }

  submit(): void {
    if (!this.createForm || this.createForm.invalid) {
      // Có thể báo lỗi nhẹ cho user
      Swal.fire('Thiếu dữ liệu', 'Vui lòng kiểm tra lại các trường bắt buộc', 'warning').then(
        (r) => {}
      );
      return;
    }

    this.loading = true;

    this.bocauHoiService.create(this.form).subscribe({
      next: (res: ResponseObject<any>) => {
        this.loading = false;
        const created = res.data;
        Swal.fire('Thành công', 'Đã tạo bộ câu hỏi mới', 'success').then(() => {
          if (created?.id) {
            this.router.navigate(['/admin/bo-cau-hoi', created.id]).then((r) => {});
          } else {
            this.router.navigate(['/admin/bo-cau-hoi']).then((r) => {});
          }
        });
      },
      error: () => {
        this.loading = false;
        Swal.fire('Lỗi', 'Không thể tạo bộ câu hỏi', 'error').then((r) => {});
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/bo-cau-hoi']);
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
    this.showCheDoDropdown = false;
  }

  selectChuDe(id: number) {
    this.form.chu_de_id = id;
    this.showChuDeDropdown = false;
  }

  getSelectedChuDeName(): string {
    if (!this.form.chu_de_id) return '-- Chọn chủ đề --';
    const selected = this.chu_de_list.find(c => c.id === this.form.chu_de_id);
    return selected ? selected.ten : '-- Chọn chủ đề --';
  }

  toggleCheDoDropdown() {
    this.showCheDoDropdown = !this.showCheDoDropdown;
    this.showChuDeDropdown = false;
  }

  selectCheDo(val: string) {
    this.form.che_do_hien_thi = val;
    this.showCheDoDropdown = false;
  }

  getSelectedCheDoName(): string {
    if (this.form.che_do_hien_thi === 'RIENG_TU') return '🔒 Private (Dùng để tổ chức thi đấu)';
    if (this.form.che_do_hien_thi === 'CONG_KHAI') return '🌍 Public (Công khai cho mọi người)';
    return '-- Chọn chế độ --';
  }

  closeAllDropdowns() {
    this.showChuDeDropdown = false;
    this.showCheDoDropdown = false;
  }
}
