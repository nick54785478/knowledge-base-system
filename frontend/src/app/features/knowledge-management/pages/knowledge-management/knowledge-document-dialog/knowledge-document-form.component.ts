import {
  Component,
  EventEmitter,
  inject,
  Input,
  OnInit,
  Output,
} from '@angular/core';
// PrimeNG 模組
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SystemMessageService } from '../../../../../shared/services/system-message.service';
import { LoadingMaskService } from '../../../../../shared/services/loading-mask.service';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Option } from '../../../../../shared/models/option.model';
import { DropdownModule } from 'primeng/dropdown';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { BaseFormCompoent } from '../../../../../shared/component/base/base-form.component';
import { OptionService } from '../../../../../shared/services/option.service';
import { DocumentService } from '../../../services/document.service';
import { lastValueFrom } from 'rxjs/internal/lastValueFrom';
import { FormAction } from '../../../../../code/enums/form-action.enum';

@Component({
  selector: 'app-knowledge-document-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DialogModule,
    DropdownModule,
    ButtonModule,
    InputTextModule,
    TextareaModule,
    AutoCompleteModule,
  ],
  providers: [LoadingMaskService],
  templateUrl: './knowledge-document-form.component.html',
  styleUrl: './knowledge-document-form.component.scss',
})
export class KnowledgeDocumentFormComponent
  extends BaseFormCompoent
  implements OnInit
{
  action: string = FormAction.ADD; // 預設新增

  // 分類下拉選單選項 (對齊主畫面的選項)
  categoryOptions: Option[] = [];

  // 標籤 (Tags) 相關變數
  filteredTags: string[] = []; // 提供給畫面的建議清單
  existingTags: string[] = []; // 系統現有的熱門標籤快取

  constructor(
    private ref: DynamicDialogRef,
    private config: DynamicDialogConfig,
    private optionService: OptionService,
    private documentService: DocumentService,
  ) {
    super();
  }

  ngOnInit(): void {
    // 2. 從 config.data 解析出傳入的參數
    const dialogParams = this.config.data || {};
    this.action = dialogParams.action || FormAction.ADD;
    const rowData = dialogParams.data || {};

    console.log('當前表單模式:', this.action);

    // 3. 初始化表單
    this.formGroup = new FormGroup({
      id: new FormControl(rowData.id || null),
      title: new FormControl(rowData.title || '', [Validators.required]),
      category: new FormControl(
        {
          value: rowData.category || '',
          disabled: this.action === FormAction.EDIT,
        },
        [Validators.required],
      ),
      author: new FormControl(rowData.author || 'Nick_Admin', [
        Validators.required,
      ]),
      // 🌟 初始化 tags 欄位，確保它一定是一個陣列
      tags: new FormControl(rowData.tags || []),
      content: new FormControl(rowData.contentSnippet || '', [
        Validators.required,
      ]),
    });

    // 載入分類
    this.optionService.getOptionsByDataType('TOPIC').subscribe((res) => {
      this.categoryOptions = res;
    });

    // 🌟 載入系統現有標籤供 AutoComplete 建議使用
    this.loadExistingTags();
  }

  /**
   * 載入系統現有標籤
   */
  loadExistingTags() {
    // 💡 TODO: 如果你的 DocumentService 實作了取得熱門標籤的 API，請解除下方註解：
    /*
    this.documentService.getPopularTags().subscribe({
      next: (tags) => { this.existingTags = tags; },
      error: (err) => { console.warn('無法載入標籤列表', err); }
    });
    */

    // 暫時給予常用的技術與業務標籤作為測試預設值
    this.existingTags = [
      'Java',
      'Spring Boot',
      'Spring AI',
      'Kafka',
      'DDD',
      'CQRS',
      '微服務',
      '資訊安全',
      '技術規範',
      '行政公告',
    ];
  }

  /**
   * 🌟 標籤的 AutoComplete 搜尋與「自定義 (Suggest + Create)」邏輯
   */
  searchTags(event: any) {
    const query = (event.query || '').trim();

    // 如果沒打字，就不做任何過濾，直接顯示部分現有標籤
    if (!query) {
      this.filteredTags = [...this.existingTags];
      return;
    }

    const lowerQuery = query.toLowerCase();

    // 1. 先從系統現有的熱門標籤中找出符合的建議
    this.filteredTags = this.existingTags.filter((tag) =>
      tag.toLowerCase().includes(lowerQuery),
    );

    // 2. 檢查現有的建議清單中，有沒有跟使用者輸入「完全一模一樣」的標籤？
    const isExactMatch = this.filteredTags.some(
      (tag) => tag.toLowerCase() === lowerQuery,
    );

    // 3. 捍衛個人意志：如果沒有一模一樣的，這是一個「新標籤」，將它強制安插到建議的第一順位
    if (!isExactMatch) {
      this.filteredTags.unshift(query);
    }
  }

  /**
   * 提交表單
   */
  /**
   * 提交表單
   */
  async submitForm() {
    this.submitted = true;

    // 1. 如果表單驗證失敗，直接中斷
    if (this.formGroup.invalid || !this.submitted) {
      return;
    }

    // 使用 getRawValue() 確保 disabled 的欄位 (如 Edit 時的 category) 也能被取回
    const payload = this.formGroup.getRawValue();

    try {
      if (this.action === FormAction.ADD) {
        await lastValueFrom(this.documentService.createDocument(payload));
      } else {
        await lastValueFrom(
          this.documentService.updateDocument(payload.id, payload),
        );
      }

      // 🌟 樂觀 UI 關鍵：把使用者剛剛填寫的最薪資料 (payload) 整包傳回給父層
      this.ref.close({
        isSuccess: true,
        action: this.action,
        data: payload,
      });
    } catch (error) {
      console.error('儲存文檔發生錯誤:', error);
    } finally {
      this.submitted = false;
    }
  }

  /**
   * 取消關閉
   */
  cancel() {
    this.ref.close();
  }
}
