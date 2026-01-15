import {CommonModule} from '@angular/common';
import {Component, OnInit, ViewChild} from '@angular/core';
import {FormsModule, NgForm} from '@angular/forms';
import Swal from 'sweetalert2';
import {ChuDe} from '../../../models/chude';
import {BoCauHoiResponse} from '../../../responses/bocauhoi/bocauhoi-response';
import {ResponseObject} from '../../../responses/response-object';
import {Base} from '../../base/base';

@Component({
  selector: 'app-bo-cau-hoi-tao-moi-bo-cau-hoi',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './tao-moi-bo-cau-hoi.html',
  styleUrl: './tao-moi-bo-cau-hoi.scss',
})
export class BoCauHoiCreate extends Base implements OnInit {
  @ViewChild('createForm') createForm!: NgForm;

  chuDes: ChuDe[] = [];
  creating = false;

  model = {
    tieu_de: '',
    mo_ta: '',
    chu_de_id: 0,
    che_do_hien_thi: 'CONG_KHAI',
    muon_tao_tra_phi: false, // Mặc định là miễn phí
  };
  showChuDeDropdown: boolean = false;

  ngOnInit(): void {
    this.loadChuDe();
  }

  /** Gọi API lấy danh sách chủ đề */
  loadChuDe() {
    this.chuDeService.getChuDe(0, 100).subscribe({
      next: (res: ResponseObject<any>) => {
        this.chuDes = res.data || [];
      },
      error: () => {
        Swal.fire('Lỗi', 'Không thể tải danh sách chủ đề', 'error').then((r) => {
        });
      },
    });
  }

  /** Submit form tạo bộ câu hỏi */
  onSubmit(form: NgForm) {
    if (form.invalid || this.model.chu_de_id === 0) {
      Swal.fire('Cảnh báo', 'Vui lòng điền đủ thông tin bắt buộc', 'warning').then(() => {
      });
      return;
    }

    this.creating = true;

    this.bocauHoiService.create(this.model).subscribe({
      next: (res: ResponseObject<BoCauHoiResponse>) => {
        const bo = res.data!;
        Swal.fire('Thành công', 'Tạo bộ câu hỏi thành công!', 'success').then(() => {
          this.router.navigate(['/bo-cau-hoi/chi-tiet-bo-cau-hoi', bo.id]).then(() => {
          });
        });
      },
      error: (err) => {
        Swal.fire('Lỗi', err.error?.message || 'Không thể tạo bộ câu hỏi', 'error').then(() => {
        });
      },
      complete: () => (this.creating = false),
    });
  }

  cancel() {
    this.router.navigateByUrl('/bo-cau-hoi/danh-sach-bo-cau-hoi').then((r) => {
    });
  }

  toggleChuDeDropdown() {
    this.showChuDeDropdown = !this.showChuDeDropdown;
  }

  selectChuDe(id: number) {
    this.model.chu_de_id = id;
    this.showChuDeDropdown = false;
  }

  closeDropdown() {
    this.showChuDeDropdown = false;
  }

  getSelectedChuDeName(): string {
    if (!this.model.chu_de_id) return '-- Chọn chủ đề phù hợp --';
    const selected = this.chuDes.find(c => c.id === this.model.chu_de_id);
    return selected ? selected.ten : '-- Chọn chủ đề phù hợp --';
  }
}
