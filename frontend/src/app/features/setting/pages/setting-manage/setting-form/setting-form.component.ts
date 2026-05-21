import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { BaseFormCompoent } from '../../../../../shared/component/base/base-form.component';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Option } from '../../../../../shared/models/option.model';
import { OptionService } from '../../../../../shared/services/option.service';

@Component({
  selector: 'app-setting-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    DropdownModule,
    TextareaModule,
  ],
  templateUrl: './setting-form.component.html',
  styleUrl: './setting-form.component.scss',
})
export class SettingFormComponent extends BaseFormCompoent implements OnInit {
  action: string = 'ADD';

  // 狀態選項
  activeFlagOptions: Option[] = [];
  dataTypeOptions: Option[] = [];

  constructor(
    private config: DynamicDialogConfig,
    private ref: DynamicDialogRef,
    private optionService: OptionService,
  ) {
    super();
  }

  ngOnInit(): void {
    console.log(this.action);
    this.optionService.getOptionsByDataType('YES_NO').subscribe((res) => {
      this.activeFlagOptions = res;
    });

    this.optionService.getDataTypes().subscribe((res) => {
      this.dataTypeOptions = res;
    });

    const dialogParams = this.config.data || {};
    this.action = dialogParams.action || 'ADD';
    const rowData = dialogParams.data || {};

    this.formGroup = new FormGroup({
      id: new FormControl(rowData.id || null),
      dataType: new FormControl(rowData.dataType || '', [Validators.required]),
      type: new FormControl(rowData.type || '', [Validators.required]),
      name: new FormControl(rowData.name || '', [Validators.required]),
      code: new FormControl(rowData.code || '', [Validators.required]),
      value: new FormControl(rowData.value || ''),
      priorityNo: new FormControl(rowData.priorityNo || 1),
      description: new FormControl(rowData.description || ''),
      // 新增時預設為 Y，但可能不需要傳給後端；編輯時必須帶上
      activeFlag: new FormControl(rowData.activeFlag || 'Y', [
        Validators.required,
      ]),
    });
  }

  /**
   * 檢核必填欄位
   * @param controlName
   * @returns
   */
  isFieldInvalid(controlName: string): boolean {
    const control = this.formGroup.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  /**
   * 提交資料
   */
  submitForm() {
    this.formGroup.markAllAsTouched();
    if (this.formGroup.valid) {
      this.ref.close(this.formGroup.getRawValue());
    }
  }

  /**
   * 取消
   */
  cancel() {
    this.ref.close();
  }
}
