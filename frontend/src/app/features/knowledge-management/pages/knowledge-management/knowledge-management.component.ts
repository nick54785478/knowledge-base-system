import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { KnowledgeDocument } from '../../models/knowlwdge-document.model';
import { DocumentService } from '../../services/document.service';
import { SystemMessageService } from '../../../../shared/services/system-message.service';
import { CommonModule } from '@angular/common';
import {
  FormControl,
  FormGroup,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ToastModule } from 'primeng/toast';
import { InputTextModule } from 'primeng/inputtext';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TextareaModule } from 'primeng/textarea';
import { ToolbarModule } from 'primeng/toolbar';
import { BaseFormTableCompoent } from '../../../../shared/component/base/base-form-table.component';
import {
  DialogService,
  DynamicDialogConfig,
  DynamicDialogRef,
} from 'primeng/dynamicdialog';
import { FormAction } from '../../../../code/enums/form-action.enum';
import { takeUntil } from 'rxjs/internal/operators/takeUntil';
import { Subject } from 'rxjs/internal/Subject';
import { DialogFormComponent } from '../../../../shared/component/dialog-form/dialog-form.component';
import { KnowledgeDocumentFormComponent } from './knowledge-document-dialog/knowledge-document-form.component';
import { finalize, map } from 'rxjs';
import { DropdownModule } from 'primeng/dropdown';
import { Option } from '../../../../shared/models/option.model';
import { LoadingMaskService } from '../../../../shared/services/loading-mask.service';
import { OptionService } from '../../../../shared/services/option.service';
import { ChangeCategoryDialogComponent } from './change-category-dialog/change-category-dialog.component';
import { ConfirmationService } from 'primeng/api';
import { DialogConfirmService } from '../../../../shared/services/dialog-confirm.service';
@Component({
  selector: 'app-knowledge-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    ToastModule,
    DropdownModule,
    ToolbarModule,
  ],
  providers: [
    DynamicDialogConfig,
    DynamicDialogRef,
    DialogService,
    SystemMessageService,
    LoadingMaskService,
    DialogConfirmService,
  ],
  templateUrl: './knowledge-management.component.html',
  styleUrl: './knowledge-management.component.scss',
})
export class KnowledgeManagementComponent
  extends BaseFormTableCompoent
  implements OnInit, OnDestroy
{
  categoryOptions: Option[] = [];
  action: string = FormAction.ADD; // 預設新增
  readonly _destroying$ = new Subject<void>(); // 用來取消訂閱
  ref: DynamicDialogRef | undefined;

  constructor(
    private documentService: DocumentService,
    private config: DynamicDialogConfig,
    public dialogService: DialogService,
    private optionService: OptionService,
    private dialogConfirmService: DialogConfirmService,
  ) {
    super();
  }

  override ngOnInit(): void {
    super.ngOnInit(); // 執行基底類的狀態初始化 (會重設表單與狀態)

    // 接收父元件傳來的參數 (區分新增或編輯)
    this.action = this.config.data?.action || FormAction.ADD;
    const initData = this.config.data?.data || {};

    // 初始化 Table 資料
    this.initTableColumns();

    this.optionService.getOptionsByDataType('TOPIC').subscribe((res) => {
      this.categoryOptions = res;
    });

    // 初始化查詢表單
    this.formGroup = new FormGroup({
      keyword: new FormControl(''),
      category: new FormControl('', Validators.required), // 預設為空 (全部)
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
   * 定義動態表格欄位參數 (對齊基底類 cols)
   **/
  private initTableColumns() {
    this.cols = [
      { field: 'highlightedTitle', header: '標題' },
      { field: 'category', header: '分類' },
      { field: 'author', header: '作者' },
      { field: 'actions', header: '操作' },
    ];
  }

  /**
   * 讀取後端資料庫文檔列表 (對齊基底類 tableData)
   */
  loadDocuments(category: string, keyword: string) {
    this.loadingMaskService.show();
    this.documentService
      .getDocuments(category, keyword)
      .pipe(
        finalize(() => {
          this.loadingMaskService.hide();
          this.submitted = false;
        }),
      )
      .subscribe({
        next: (data) => {
          this.messageService.showSuccess('success', '查詢知識文件成功');
          this.tableData = data;
        },
        error: (err) => {
          this.messageService.showError('error', err?.message);
          console.warn('無法載入文檔列表，請確認後端 RESTful API 狀態', err);
        },
      });
  }

  /**
   * 執行查詢
   */
  onSearch() {
    this.submitted = true;
    console.log('submitted: ', this.submitted);
    if (!this.submitted || this.formGroup.invalid) {
      return;
    }

    const keyword = this.formGroup.get('keyword')?.value || '';
    const category = this.formGroup.get('category')?.value || '';

    // 呼叫載入方法
    this.loadDocuments(category, keyword);
  }

  /**
   * 開啟新增表單
   */
  showAddDialog() {
    this.openFormDialog(FormAction.ADD);
  }

  /**
   * 開啟 Dialog 表單
   * @returns DynamicDialogRef
   */
  /**
   * 開啟 Dialog 表單
   * @returns DynamicDialogRef
   */
  openFormDialog(formAction?: FormAction, data?: any): DynamicDialogRef {
    this.dialogOpened = true;

    this.ref = this.dialogService.open(DialogFormComponent, {
      header: formAction === FormAction.ADD ? '新增知識文檔' : '編輯知識文檔',
      width: '60%',
      contentStyle: { overflow: 'auto' },
      baseZIndex: 10000,
      maximizable: true,
      data: {
        action: formAction,
        data: data, // 這裡是點擊編輯時傳入的原始 rowData
      },
      templates: {
        content: KnowledgeDocumentFormComponent,
      },
    });

    // Dialog 關閉後要做的事情
    this.ref?.onClose
      .pipe(takeUntil(this._destroying$))
      .subscribe((result: any) => {
        console.log('關閉 Dialog');
        this.dialogOpened = false;

        // 🌟 確認有成功回傳資料 (代表是按下儲存，而不是按取消或右上角的叉叉)
        if (result && result.isSuccess) {
          this.messageService.showSuccess(
            'success',
            '文檔儲存成功，系統正於背景同步更新檢索庫',
          );

          const payload = result.data; // 這是表單關閉時傳回來的最新表單值

          // 🌟 情境 A：如果是編輯 (EDIT)，啟動 Optimistic UI 直接修改畫面陣列
          if (result.action === FormAction.EDIT || result.action === 'edit') {
            const targetDoc = this.tableData.find(
              (doc: any) => doc.id === payload.id,
            );

            if (targetDoc) {
              // 1. 更新標題 (同步更新 raw 欄位與 highlighted 欄位)
              targetDoc.title = payload.title;
              targetDoc.highlightedTitle = payload.title; // 🌟 關鍵：讓 Table 瞬間顯示新標題 (解除高亮狀態)

              // 2. 更新其他屬性
              targetDoc.category = payload.category;
              targetDoc.author = payload.author;
              targetDoc.tags = payload.tags;

              // 3. 更新內文摘要 (同步更新 raw 欄位與 highlighted 欄位)
              targetDoc.contentSnippet =
                payload.content.length > 300
                  ? payload.content.substring(0, 300) + '...'
                  : payload.content;

              targetDoc.highlightedContentSnippet = targetDoc.contentSnippet; // 🌟 關鍵：一併更新內文摘要
            }
          }
          // 🌟 情境 B：如果是新增 (ADD)，因為前端不知道 UUID，所以等待 1.5 秒讓 CDC 跑完再刷新列表
          else if (
            result.action === FormAction.ADD ||
            result.action === 'add'
          ) {
            setTimeout(() => {
              this.onSearch(); // 呼叫你原本寫好的搜尋方法
            }, 1500);
          }
        }
      });

    return this.ref;
  }

  /**
   * Table Action 按鈕按下去的時候要把該筆資料記錄下來。
   * @param rowData 點選的資料
   */
  override clickRowActionMenu(rowData: any): void {
    console.log(rowData);
    // console.log('clickRowActionMenu rowData = ' + JSON.stringify(rowData));
    this.rowCurrentData = rowData;
    console.log(this.rowCurrentData);

    // 開啟 Dialog
    this.openFormDialog(FormAction.EDIT, this.rowCurrentData);
  }

  /**
   * 執行儲存動作與防禦性阻斷
   **/
  saveDocument() {
    this.submitted = true;

    // 表單檢核未通過時，利用基底類別或本地驗證阻斷發送
    if (this.formGroup.invalid || !this.submitted) {
      this.messageService.showError('驗證失敗', '請確認必填欄位皆已正確填寫');
      return;
    }

    // 透過基底類別封裝或直接讀取表單值
    const payload = this.formGroup.value;

    this.loadingMaskService.show();

    this.documentService
      .createDocument(payload)
      .pipe(
        finalize(() => {
          this.submitted = false;
          this.loadingMaskService.show();
        }),
      )
      .subscribe({
        next: () => {
          // 使用基底類別注入的系統訊息服務提示使用者
          this.messageService.showSuccess(
            '同步成功',
            '文檔已安全寫入資料庫，CDC 基礎建設正於背景同步向量特徵',
          );
          this.dialogOpened = false; // 關閉彈窗
          // this.loadDocuments(); // 重新整理 Table 清單
        },
        error: (err) => {
          this.messageService.showError('系統錯誤', '寫入知識庫失敗');
          console.error(err);
        },
      });
  }

  /**
   * 取消動作
   **/
  cancelAction() {
    this.dialogOpened = false;
    this.submitted = false;
  }

  /**
   * 清空資料
   */
  onReset() {
    this.formGroup.reset({ keyword: '', category: '' });
    this.tableData = [];
    // this.loadDocuments(''); // 重新載入全部資料
  }

  /**
   * 開啟修改 Category Dialog
   * @param rowData
   */
  openChangeCategoryDialog(rowData: any) {
    const ref = this.dialogService.open(DialogFormComponent, {
      header: '變更文檔分類',
      width: '450px', // 這個迷你任務不需要太寬
      data: {
        id: rowData.id,
        title: rowData.title,
        currentCategory: rowData.category,
      },
      templates: {
        content: ChangeCategoryDialogComponent,
      },
    });

    ref.onClose.pipe(takeUntil(this._destroying$)).subscribe((result: any) => {
      if (result && result.isSuccess) {
        this.messageService.showSuccess(
          'success',
          '分類變更成功，系統正在非同步更新 AI 向量庫',
        );

        // 🌟 樂觀 UI 更新 (Optimistic UI) 🌟
        const newCategory = result.newCategory;
        const currentFilterCategory = this.formGroup.get('category')?.value;

        // 情境 A：使用者目前有選擇「特定分類」進行過濾
        // 如果新分類不等於當前過濾的分類，代表這篇文章應該從畫面上「消失」
        if (
          currentFilterCategory &&
          currentFilterCategory !== '全部' &&
          currentFilterCategory !== newCategory
        ) {
          this.tableData = this.tableData.filter(
            (doc) => doc.id !== rowData.id,
          );
        }
        // 情境 B：使用者目前看的是「全部」或是剛好與新分類相同
        // 直接在前端把該筆資料的分類文字替換掉
        else {
          const targetDoc = this.tableData.find((doc) => doc.id === rowData.id);
          if (targetDoc) {
            targetDoc.category = newCategory;
          }
        }

        // 注意：我們完全移除了 this.loadDocuments()，節省了一次無效的 API 請求！
      }
    });
  }

  /**
   * 刪除知識文檔
   * @param uuid 要刪除的文檔 ID
   */
  onDelete(uuid: string) {
    // 1. 取得文檔標題，做為提示訊息的 hint
    const targetDoc = this.tableData.find((doc) => doc.id === uuid);
    const docTitle = targetDoc ? targetDoc.title : '此文檔';

    // 2. 呼叫客製化的 Confirm Service
    this.dialogConfirmService.confirmDelete(
      () => {
        // ========== 這是按下 YES (確認) 後要執行的 acceptCallback ==========
        this.loadingMaskService.show();

        this.documentService
          .deleteDocument(uuid)
          .pipe(
            finalize(() => {
              this.loadingMaskService.hide();
            }),
          )
          .subscribe({
            next: () => {
              this.messageService.showSuccess(
                '刪除成功',
                '文檔已成功刪除，系統正於背景同步更新檢索庫',
              );

              // 樂觀 UI 更新：直接從畫面陣列中剔除該筆資料
              this.tableData = this.tableData.filter((doc) => doc.id !== uuid);
            },
            error: (err) => {
              this.messageService.showError(
                '刪除失敗',
                err?.message || '發生未知錯誤',
              );
              console.error('刪除文檔失敗:', err);
            },
          });
      },
      docTitle, // 傳入 hint
    );
  }
}
