import {CommonModule} from '@angular/common';
import {Component, OnInit, ViewChild} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import Swal from 'sweetalert2';
import {ChuDe} from '../../../models/chude';
import {BoCauHoiResponse} from '../../../responses/bocauhoi/bocauhoi-response';
import {ResponseObject} from '../../../responses/response-object';
import {Base} from '../../base/base';

@Component({
  selector: 'app-bo-cau-hoi-sua-bo-cau-hoi',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sua-bo-cau-hoi.html',
  styleUrl: './sua-bo-cau-hoi.scss',
})
export class BoCauHoiEdit extends Base implements OnInit {
  @ViewChild('editForm') editForm!: NgForm;
  boCauHoiId!: number;
  dto: any = {
    tieu_de: '',
    mo_ta: '',
    chu_de_id: 0,
    che_do_hien_thi: 'RIENG_TU',
  };
  loading = false;
  saving = false;
  chuDes: ChuDe[] = [];
  currentStatus: string = '';

  // 1. Biến điều khiển Dropdown
  showChuDeDropdown: boolean = false;

  ngOnInit(): void {
    this.boCauHoiId = Number(this.route.snapshot.paramMap.get('id'));
    this.fetchChuDes();
    this.fetchBoCauHoi();
  }

  fetchChuDes() {
    this.chuDeService.getChuDe(0, 100).subscribe({
      next: (res: ResponseObject<any>) => {
        this.chuDes = res.data || [];
      },
      error: () => {
        Swal.fire('Lỗi', 'Không thể tải danh sách chủ đề', 'error');
      },
    });
  }

  fetchBoCauHoi() {
    this.loading = true;
    this.bocauHoiService.getById(this.boCauHoiId).subscribe({
      next: (res: ResponseObject<BoCauHoiResponse>) => {
        const d = res.data!;
        this.dto = {
          tieu_de: d.tieu_de,
          mo_ta: d.mo_ta,
          chu_de_id: d.chu_de_id,
          che_do_hien_thi: d.che_do_hien_thi,
        };
        this.currentStatus = d.trang_thai;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        Swal.fire('Lỗi', 'Không thể tải dữ liệu bộ câu hỏi', 'error').then(() => {
          this.router.navigateByUrl('/bo-cau-hoi/danh-sach-bo-cau-hoi');
        });
      },
    });
  }

  toggleChuDeDropdown() {
    this.showChuDeDropdown = !this.showChuDeDropdown;
  }

  selectChuDe(id: number) {
    this.dto.chu_de_id = id;
    this.showChuDeDropdown = false;
  }

  getSelectedChuDeName(): string {
    if (!this.dto.chu_de_id) return '-- Chọn chủ đề --';
    const selected = this.chuDes.find(c => c.id === this.dto.chu_de_id);
    return selected ? selected.ten : '-- Chọn chủ đề --';
  }

  closeDropdown() {
    this.showChuDeDropdown = false;
  }

  onSubmit(form: NgForm) {
    if (form.invalid || !this.dto.chu_de_id) { // Check thêm chu_de_id vì custom dropdown ko tự validate form
      Swal.fire('Cảnh báo', 'Vui lòng nhập đầy đủ thông tin', 'warning');
      return;
    }

    this.saving = true;
    this.bocauHoiService.update(this.boCauHoiId, this.dto).subscribe({
      next: () => {
        Swal.fire('Thành công', 'Cập nhật bộ câu hỏi thành công', 'success').then(() => {
          this.router.navigate(['/bo-cau-hoi/chi-tiet-bo-cau-hoi', this.boCauHoiId]);
        });
      },
      error: (err) => {
        Swal.fire('Lỗi', err.error?.message || 'Không thể cập nhật bộ câu hỏi', 'error');
      },
      complete: () => (this.saving = false),
    });
  }

  cancel() {
    this.router.navigateByUrl('/bo-cau-hoi/danh-sach-bo-cau-hoi');
  }
}
