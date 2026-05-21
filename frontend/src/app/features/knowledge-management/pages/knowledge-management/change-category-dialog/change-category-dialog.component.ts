import { Component, OnInit } from '@angular/core';
import { BaseFormCompoent } from '../../../../../shared/component/base/base-form.component';
import { lastValueFrom } from 'rxjs/internal/lastValueFrom';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { OptionService } from '../../../../../shared/services/option.service';
import { DocumentService } from '../../../services/document.service';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

@Component({
  selector: 'app-change-category-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
  ],
  templateUrl: './change-category-dialog.component.html',
  styleUrl: './change-category-dialog.component.scss',
})
export class ChangeCategoryDialogComponent
  extends BaseFormCompoent
  implements OnInit
{
  documentId: string = '';
  documentTitle: string = '';
  categoryOptions: any[] = [];
  categoryControl = new FormControl('', Validators.required);

  constructor(
    private ref: DynamicDialogRef,
    private config: DynamicDialogConfig,
    private optionService: OptionService,
    private documentService: DocumentService,
  ) {
    super();
  }

  ngOnInit() {
    this.documentId = this.config.data.id;
    this.documentTitle = this.config.data.title;
    // 預設選中原本的分類
    this.categoryControl.setValue(this.config.data.currentCategory);

    this.optionService.getOptionsByDataType('TOPIC').subscribe((res) => {
      this.categoryOptions = res;
    });
  }

  async submit() {
    if (this.categoryControl.invalid) return;

    // 防呆：如果根本沒改分類，直接關閉當作成功
    if (this.categoryControl.value === this.config.data.currentCategory) {
      this.ref.close(false);
      return;
    }

    this.submitted = true;
    try {
      await lastValueFrom(
        this.documentService.changeCategory(
          this.documentId,
          this.categoryControl.value!,
        ),
      );
      this.ref.close({
        isSuccess: true,
        newCategory: this.categoryControl.value,
      });
    } catch (error) {
      console.error('變更分類失敗', error);
    } finally {
      this.submitted = false;
    }
  }

  cancel() {
    this.ref.close(false);
  }
}
