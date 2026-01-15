import {CommonModule} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {FormsModule} from '@angular/forms';
import Swal from 'sweetalert2';
import {ChuDe} from '../../../models/chude';
import {KhoaHoiResponse} from '../../../responses/khoahoc/khoa-hoi-response';
import {PageResponse} from '../../../responses/page-response';
import {ResponseObject} from '../../../responses/response-object';
import {Base} from '../../base/base';

@Component({
  selector: 'app-danh-sach-khoa-hoc',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './danh-sach-khoa-hoc.html',
  styleUrl: './danh-sach-khoa-hoc.scss',
})
export class DanhSachKhoaHoc extends Base implements OnInit {
  loading = false;
  keyword = '';
  trangThai = '';
  chuDeId = 0;
  page = 0;
  limit = 6;
  sortOrder = 'NEWEST';
  totalPages = 0;
  currentUserId: number = 0;
  items: KhoaHoiResponse[] = [];
  minRating: number | undefined = undefined;

  chuDes: ChuDe[] = [];
  readonly trangThaiOptions = [
    {value: '', label: 'Tất cả trạng thái'},
    {value: 'CONG_KHAI', label: 'Đã xuất bản'},
    {value: 'BAN_NHAP', label: 'Bản nháp'},
    {value: 'LUU_TRU', label: 'Đã lưu trữ'},
  ];
  readonly ratingOptions = [
    {value: undefined, label: '⭐ Tất cả đánh giá'},
    {value: 4, label: '⭐ 4+ sao'},
    {value: 3, label: '⭐ 3+ sao'},
    {value: 2, label: '⭐ 2+ sao'},
    {value: 1, label: '⭐ 1+ sao'},
  ];
  isMobileFilterOpen: boolean = false;

  showSortDropdown = false;
  showChuDeDropdown = false;
  showTrangThaiDropdown = false;

  ngOnInit() {
    this.currentUserId = this.tokenService.getUserId();
    this.loadData();
    this.loadChuDe();
  }

  loadData() {
    this.loading = true;
    this.khoaHocService
      .getAll(this.keyword, this.chuDeId, this.trangThai, this.sortOrder, this.page, this.limit, this.minRating, undefined)
      .subscribe({
        next: (res: ResponseObject<PageResponse<KhoaHoiResponse>>) => {
          const data = res.data!;
          this.items = data.items ?? [];
          this.totalPages = data.totalPages;
          this.loading = false;
        },
        error: () => {
          this.loading = false;
          Swal.fire('Lỗi', 'Không thể tải danh sách khóa học', 'error');
        },
      });
  }

  loadChuDe() {
    this.chuDeService.getChuDe(0, 100).subscribe({
      next: (res: ResponseObject<any>) => {
        this.chuDes = res.data || [];
      },
      error: () => {
        console.error('Không thể tải danh sách chủ đề');
      },
    });
  }

  applyFilter() {
    this.page = 0;
    this.loadData();
  }

  getVisiblePages(): number[] {
    const visible: number[] = [];
    const maxVisible = 5;
    const total = this.totalPages;

    if (total <= maxVisible) {
      return Array.from({length: total}, (_, i) => i);
    }

    const start = Math.max(0, this.page - 2);
    const end = Math.min(total - 1, this.page + 2);

    if (start > 0) visible.push(0);
    if (start > 1) visible.push(-1); // -1 là dấu ...

    for (let i = start; i <= end; i++) visible.push(i);

    if (end < total - 2) visible.push(-2); // -2 là dấu ...
    if (end < total - 1) visible.push(total - 1);

    return visible;
  }

  changePage(newPage: number) {
    if (newPage >= 0 && newPage < this.totalPages) {
      this.page = newPage;
      this.loadData();
    }
  }

  navigateToDetail(id: number) {
    this.router.navigate(['/khoa-hoc', id]);
  }

  navigateToCreate() {
    this.router.navigate(['/khoa-hoc/tao-moi']);
  }

  navigateToEdit(id: number) {
    this.router.navigate(['/khoa-hoc', id, 'sua']);
  }

  isOwner(khoaHoc: KhoaHoiResponse): boolean {
    return khoaHoc.nguoi_tao_id === this.currentUserId;
  }

  isAdmin(): boolean {
    return this.tokenService.getRoles().includes('ROLE_ADMIN');
  }

  canEditOrDelete(khoaHoc: KhoaHoiResponse): boolean {
    return this.isAdmin() || this.isOwner(khoaHoc);
  }

  onDelete(id: number) {
    Swal.fire({
      title: 'Xác nhận xóa?',
      text: 'Bạn có chắc muốn xóa khóa học này không?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Xóa',
      cancelButtonText: 'Hủy',
    }).then((result) => {
      if (result.isConfirmed) {
        this.khoaHocService.delete(id).subscribe({
          next: () => {
            Swal.fire('Thành công', 'Xóa khóa học thành công', 'success').then(() => {
              this.loadData();
            });
          },
          error: () => Swal.fire('Lỗi', 'Không thể xóa khóa học', 'error'),
        });
      }
    });
  }

  toggleMobileFilter() {
    this.isMobileFilterOpen = !this.isMobileFilterOpen;
  }

  closeAllDropdowns() {
    this.showSortDropdown = false;
    this.showChuDeDropdown = false;
    this.showTrangThaiDropdown = false;
  }

  // --- XỬ LÝ DROPDOWN SẮP XẾP ---
  toggleSortDropdown() {
    const wasOpen = this.showSortDropdown;
    this.closeAllDropdowns();
    this.showSortDropdown = !wasOpen;
  }

  selectSort(value: string) {
    this.sortOrder = value;
    this.closeAllDropdowns();
    this.applyFilter();
  }

  getSortLabel(): string {
    switch (this.sortOrder) {
      case 'NEWEST':
        return '🕐 Mới nhất';
      case 'OLDEST':
        return '📅 Cũ nhất';
      case 'RATING_DESC':
        return '⭐ Đánh giá cao';
      case 'RATING_ASC':
        return '⭐ Đánh giá thấp';
      default:
        return 'Sắp xếp';
    }
  }

  // --- XỬ LÝ DROPDOWN CHỦ ĐỀ ---
  toggleChuDeDropdown() {
    const wasOpen = this.showChuDeDropdown;
    this.closeAllDropdowns();
    this.showChuDeDropdown = !wasOpen;
  }

  selectChuDe(id: number) {
    this.chuDeId = id;
    this.closeAllDropdowns();
    this.applyFilter();
  }

  getChuDeLabel(): string {
    if (this.chuDeId === 0) return '📚 Tất cả chủ đề';
    const cd = this.chuDes.find(c => c.id === this.chuDeId);
    return cd ? cd.ten : 'Chọn chủ đề';
  }

  // --- XỬ LÝ DROPDOWN TRẠNG THÁI ---
  toggleTrangThaiDropdown() {
    const wasOpen = this.showTrangThaiDropdown;
    this.closeAllDropdowns();
    this.showTrangThaiDropdown = !wasOpen;
  }

  selectTrangThai(value: string) {
    this.trangThai = value;
    this.closeAllDropdowns();
    this.applyFilter();
  }

  getTrangThaiLabel(): string {
    const opt = this.trangThaiOptions.find(o => o.value === this.trangThai);
    return opt ? opt.label : 'Tất cả trạng thái';
  }
}
