import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {environment} from '../../../environments/environment';

@Component({
  selector: 'app-address-selector',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './address-selector.html',
  styleUrl: './address-selector.scss'
})
export class AddressSelector implements OnInit {
  private readonly api = `${environment.apiBaseUrl}/provinces`;

  provinces: any[] = [];
  wards: any[] = [];
  filteredWards: any[] = [];

  selectedProvince = '';
  selectedWard = '';

  // 1. Biến điều khiển Dropdown
  showProvinceDropdown = false;
  showWardDropdown = false;

  @Output() addressChange = new EventEmitter<string>();

  constructor(private http: HttpClient) {
  }

  ngOnInit(): void {
    this.loadProvinces();
    this.loadWards();
  }

  loadProvinces(): void {
    this.http.get<any[]>(`${this.api}/p`).subscribe({
      next: data => (this.provinces = data),
      error: err => console.error('Lỗi tải provinces:', err)
    });
  }

  loadWards(): void {
    this.http.get<any[]>(`${this.api}/w`).subscribe({
      next: data => (this.wards = data),
      error: err => console.error('Lỗi tải wards:', err)
    });
  }

  // 2. Xử lý Dropdown Tỉnh/Thành
  toggleProvinceDropdown() {
    this.showProvinceDropdown = !this.showProvinceDropdown;
    this.showWardDropdown = false;
  }

  selectProvince(code: string) {
    this.selectedProvince = code;
    this.showProvinceDropdown = false;

    // Logic cũ: Filter xã theo tỉnh
    this.filteredWards = this.wards.filter(w => w.province_code == this.selectedProvince);
    this.selectedWard = ''; // Reset xã
    this.emitAddress();
  }

  getSelectedProvinceName(): string {
    const p = this.provinces.find(item => item.code == this.selectedProvince);
    return p ? p.name : '-- Chọn Tỉnh/TP --';
  }

  // 3. Xử lý Dropdown Phường/Xã
  toggleWardDropdown() {
    // Chỉ mở nếu đã chọn tỉnh (có danh sách xã)
    if (this.filteredWards.length === 0 && !this.selectedProvince) return;

    this.showWardDropdown = !this.showWardDropdown;
    this.showProvinceDropdown = false;
  }

  selectWard(code: string) {
    this.selectedWard = code;
    this.showWardDropdown = false;
    this.emitAddress();
  }

  getSelectedWardName(): string {
    const w = this.filteredWards.find(item => item.code == this.selectedWard);
    return w ? w.name : '-- Chọn Phường/Xã --';
  }

  // 4. Đóng tất cả khi click ra ngoài
  closeAllDropdowns() {
    this.showProvinceDropdown = false;
    this.showWardDropdown = false;
  }

  emitAddress(): void {
    const provinceName = this.provinces.find(p => p.code == this.selectedProvince)?.name || '';
    const wardName = this.filteredWards.find(w => w.code == this.selectedWard)?.name || '';

    // Chỉ emit khi có dữ liệu, tránh undefined
    const result = [wardName, provinceName].filter(Boolean).join(', ');
    this.addressChange.emit(result);
  }
}
