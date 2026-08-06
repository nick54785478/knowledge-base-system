import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { BaseFormTableCompoent } from '../../../../shared/component/base/base-form-table.component';
import { SettingService } from '../../services/setting.service';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';
import { ConfirmationService } from 'primeng/api';
import { Subject } from 'rxjs/internal/Subject';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { lastValueFrom } from 'rxjs/internal/lastValueFrom';
import { SystemMessageService } from '../../../../shared/services/system-message.service';
import { SettingFormComponent } from './setting-form/setting-form.component';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';
import { PRIME_COMPONENTS } from '../../../../shared/shared-primeng';
import { CommonModule } from '@angular/common';
import { TableModule } from 'primeng/table';
import { DropdownModule } from 'primeng/dropdown';
import { OptionService } from '../../../../shared/services/option.service';
import { Option } from '../../../../shared/models/option.model';
import { switchMap } from 'rxjs/internal/operators/switchMap';
import { of } from 'rxjs/internal/observable/of';
import { tap } from 'rxjs/internal/operators/tap';
import { catchError } from 'rxjs/internal/operators/catchError';
import { DialogConfirmService } from '../../../../shared/services/dialog-confirm.service';
import { Dialog } from 'primeng/dialog';

@Component({
  selector: 'app-setting-manage',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ReactiveFormsModule,
    DropdownModule,
    PRIME_COMPONENTS,
  ],
  providers: [DialogService, DialogConfirmService],
  templateUrl: './setting-manage.component.html',
  styleUrl: './setting-manage.component.scss',
})
export class SettingManageComponent
  extends BaseFormTableCompoent
  implements OnInit, OnDestroy
{
  private _destroying$ = new Subject<void>();

  activeFlagOptions: Option[] = [];
  dataTypeOptions: Option[] = [];
  typeOptions: Option[] = [];
  ref: DynamicDialogRef | undefined;

  constructor(
    private optionService: OptionService,
    private settingService: SettingService,
    private dialogService: DialogService,
    private dialogConfirmService: DialogConfirmService,
  ) {
    super();
  }

  override async ngOnInit() {
    super.ngOnInit();

    this.cols = [
      { field: 'dataType', header: '資料類型' },
      { field: 'type', header: '種類' },
      { field: 'name', header: '名稱' },
      { field: 'code', header: '代碼' },
      { field: 'value', header: '值' },
      { field: 'priorityNo', header: '順序' },
      { field: 'activeFlag', header: '狀態' },
      { field: 'actions', header: '操作' },
    ];

    this.formGroup = new FormGroup({
      dataType: new FormControl(''),
      type: new FormControl({ value: '', disabled: true }),
      name: new FormControl(''),
      activeFlag: new FormControl(''),
    });

    await this.loadData();

    // 2. 建立 RxJS 響應式監聽管線
    this.setupDropdownLinkage();

    this.optionService.getOptionsByDataType('YES_NO').subscribe((res) => {
      this.activeFlagOptions = res;
    });

    this.optionService.getDataTypes().subscribe((res) => {
      this.dataTypeOptions = res;
    });
  }

  override ngOnDestroy(): void {
    // 保證組件銷毀時關閉 Dialog
    if (this.ref) {
      this.ref.close();
    }
    this._destroying$.next(); // 確保 subject 發送訊號 (你原本寫 this._destroying$.closed 是沒有作用的喔！)
    this._destroying$.unsubscribe();
  }

  /**
   * 載入資料
   */
  async loadData() {
    const { dataType, type, name, activeFlag } = this.formGroup.value;
    try {
      const data = await lastValueFrom(
        this.settingService.summary(dataType, type, name, activeFlag),
      );
      this.tableData = data;
    } catch (e) {
      this.messageService.showError('錯誤', '查詢失敗');
    }
  }

  /**
   * 搜尋資料
   */
  onSearch() {
    this.loadData();
  }

  /**
   * 重置資料
   */
  onReset() {
    this.formGroup.reset();
    this.tableData = [];
  }

  /**
   * 開啟 Dialog
   * @param action 動作(ADD 或 EDIT)
   * @param data 資料
   */
  openFormDialog(action: 'ADD' | 'EDIT', data?: any) {
    this.ref = this.dialogService.open(SettingFormComponent, {
      header: action === 'ADD' ? '新增參數設定' : '編輯參數設定',
      width: '700px',
      data: { action, data },
    });

    this.ref.onClose
      .pipe(takeUntil(this._destroying$))
      .subscribe(async (formData: any) => {
        if (formData) {
          try {
            if (action === 'ADD') {
              await lastValueFrom(this.settingService.create(formData));
              this.messageService.showSuccess('success', '新增成功');
            } else {
              await lastValueFrom(
                this.settingService.update(formData.id, formData),
              );
              this.messageService.showSuccess('success', '更新成功');
            }
            await this.loadData();
          } catch (e) {
            this.messageService.showError('error', '儲存失敗');
          }
        }
      });
  }

  /**
   * 刪除設定
   * @param rowData 列資料
   */
  deleteSetting(rowData: any) {
    this.dialogConfirmService.confirmDelete(
      async () => {
        // ========== 這是按下「確認刪除」後執行的 acceptCallback ==========
        try {
          await lastValueFrom(this.settingService.delete(rowData.id));
          this.messageService.showSuccess('success', '刪除成功');

          // 重新載入表格資料
          await this.loadData();
        } catch (e) {
          this.messageService.showError('error', '刪除失敗');
          console.error('刪除設定發生錯誤:', e);
        }
      },
      rowData.name, // 🌟 傳入設定名稱做為提示 (hint)
    );
  }

  /**
   * 設定下拉選單的連動邏輯 (DataType -> Type)
   */
  private setupDropdownLinkage() {
    this.formGroup
      .get('dataType')
      ?.valueChanges.pipe(
        takeUntil(this._destroying$),

        // 【步驟 1：同步副作用】打 API 前先清理畫面狀態與防呆
        tap((dataTypeValue) => {
          const typeControl = this.formGroup.get('type');

          // (可選) 如果切換主類別時，希望下方的 Table 資料也先淨空，可以解開下方註解
          // this.tableData = [];

          if (dataTypeValue) {
            typeControl?.enable();
            typeControl?.setValue(null); // 清空舊值
            typeControl?.markAsUntouched();
            typeControl?.markAsPristine();
          } else {
            // 沒選 DataType 時，清空並鎖定 Type
            typeControl?.setValue(null);
            typeControl?.disable();
            this.typeOptions = [];
          }
        }),

        // 【步驟 2：非同步 API】使用 switchMap 避免 Race Condition
        switchMap((dataTypeValue) => {
          if (!dataTypeValue) {
            return of([]); // 沒值就不打 API，直接回傳空陣列往下流
          }

          return this.optionService.getSettingTypes(dataTypeValue).pipe(
            // 關鍵防護：錯誤攔截寫在內部，管線才不會斷掉
            catchError((error) => {
              console.error('取得 Type 選單發生錯誤:', error);
              this.messageService.showError('error', '無法取得設定種類選單');
              return of([]);
            }),
          );
        }),
      )
      .subscribe({
        // 【步驟 3：接收最終乾淨的資料並轉換格式】
        next: (res: any[]) => {
          this.typeOptions = res;
        },
      });
  }
}
