import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

/**
 * 定義基礎的 Form 表單 Component
 */
@Component({
  selector: 'app-base-form-compoent',
  standalone: true,
  imports: [],
  providers: [],
  template: '',
})
export abstract class BaseFormCompoent {
  /**
   * 定義 Form Group
   * */
  protected formGroup!: FormGroup;

  /**
   * 用於 Submit 用
   */
  protected submitted: boolean = false;

  /**
   * 表單動作
   */
  protected formAction!: string;

  constructor() {}

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
}
