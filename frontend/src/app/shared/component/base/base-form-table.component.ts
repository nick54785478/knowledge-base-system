import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MenuItem } from 'primeng/api';
import { LoadingMaskService } from '../../services/loading-mask.service';
import { SystemMessageService } from '../../services/system-message.service';

/**
 * 定義基礎的 Form 表單及 Table 表格 Component
 */
@Component({
  selector: 'app-base-form-table-compoent',
  standalone: true,
  imports: [],
  providers: [],
  template: '',
})
export abstract class BaseFormTableCompoent implements OnInit, OnDestroy {
  protected loadingMaskService = inject(LoadingMaskService);
  protected messageService = inject(SystemMessageService);
  /**
   * 定義 Form Group
   * */
  protected formGroup!: FormGroup;

  /**
   * 用於 Submit 用
   * */
  protected submitted: boolean = false;
  /**
   * 表單動作
   * */
  protected formAction!: string;

  /**
   * 動態定義表格欄位參數
   */
  cols: any[] = [];

  /**
   * 表格資料
   */
  tableData: any[] = [];

  /**
   * 上方頁簽(若有需要使用時可使用這個參數)
   * */
  detailTabs: MenuItem[] = [];

  /**
   * 現在選取的那一筆
   */
  rowCurrentData: any;

  /**
   * 是否開啟 Dialog
   */
  protected dialogOpened: boolean = false;

  /**
   * Table Row Actions 選單
   */
  rowActionMenu: MenuItem[] = [];

  constructor() {}

  /**
   * OnInit 初始化動作
   */
  ngOnInit(): void {
    this.resetState(); // 初始化
  }

  /**
   * OnDestroy 用於 Component 生命週期結束前的動作
   */
  ngOnDestroy(): void {}

  /**
   * Patch FormGroup 的值
   * @param data
   */
  patchFormGroupValue(data?: any) {}

  /**
   * 取得 FormControl。
   * @param formControlName formControlNameformControl 的名稱
   * @returns FormControl
   */
  formControl(formControlName: string): FormControl {
    return this.formGroup.get(formControlName) as FormControl;
  }

  /**
   * 判斷 formControl 欄位是否有錯誤。
   * @param formControlName formControl 的名稱
   * @returns boolean 欄位是否有錯誤
   */
  formControlInvalid(formControlName: string): boolean {
    const formControl = this.formGroup.get(formControlName);
    if (formControl) {
      return formControl.invalid && (formControl.dirty || this.submitted);
    } else {
      return false;
    }
  }

  /**
   * 紀錄該筆資料
   * @param rowData 點選的資料
   */
  clickRowActionMenu(rowData: any): void {
    this.rowCurrentData = rowData;
  }

  /**
   * 初始化狀態
   */
  private resetState() {
    this.dialogOpened = false;
    this.detailTabs = [];
    this.tableData = [];
    this.cols = [];
    this.formGroup = new FormGroup({});
    this.submitted = false;
  }
}
