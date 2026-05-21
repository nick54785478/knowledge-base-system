import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../../../environments/environment';
import { CreateKnowledgeDocumentResource } from '../models/create-knowlwdge-document-request.model';
import { KnowledgeDocumentGottenResource } from '../models/knowlwdge-document.gotten.response.model';
import { map } from 'rxjs';
import { KnowledgeDocumentGottenData } from '../models/knowlwdge-document.gotten.model';
import { UpdateKnowledgeDocumentResource } from '../models/update-knowlwdge-document-request.model';

@Injectable({
  providedIn: 'root',
})
export class DocumentService {
  // 對齊你的 Spring Boot 寫入 API 路徑
  private readonly baseApiUrl = environment.apiEndpoint;
  private apiUrl = this.baseApiUrl + '/knowledge-documents';

  constructor(private http: HttpClient) {}

  /**
   * 1. 搜尋知識文件 (Query Side)
   * 透過 Elasticsearch 進行高效能檢索
   * @param category 分類
   * @param keyword 關鍵字
   */
  getDocuments(
    category: string,
    keyword: string,
  ): Observable<KnowledgeDocumentGottenData[]> {
    const url = this.apiUrl + '/search';
    let params = new HttpParams()
      .set('category', category ? category : '')
      .set('q', keyword ? keyword : '');

    return this.http.get<KnowledgeDocumentGottenResource>(url, { params }).pipe(
      map((res) => {
        return res?.data || []; // 防呆：如果 data 為空則回傳空陣列
      }),
    );
  }

  /**
   * 2. 新增文檔 (Command Side)
   * 會觸發後端 CDC 與 AI 向量化
   * @param doc 建立文檔的 DTO
   */
  createDocument(doc: CreateKnowledgeDocumentResource): Observable<any> {
    return this.http.post(this.apiUrl, doc);
    // 註：後端回傳 201 Created，且 Header 帶有 Location。
    // 如果前端需要立刻跳轉，你可以用 { observe: 'response' } 去抓 location header，
    // 但如果只是要在背景觸發 AI，這樣寫就足夠了。
  }

  /**
   * 3. 修改知識庫文檔 (Command Side)
   * @param id 文檔唯一識別碼
   * @param doc 更新文檔的 DTO
   */
  updateDocument(
    id: string,
    doc: UpdateKnowledgeDocumentResource,
  ): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, doc);
  }

  /**
   * 4. 刪除知識庫文檔 (Command Side)
   * @param id 文檔唯一識別碼
   */
  deleteDocument(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  /**
   * 變更知識庫文檔分類
   */
  changeCategory(id: string, newCategory: string): Observable<any> {
    const payload = { newCategory };
    return this.http.patch(
      `/api/v1/knowledge-documents/${id}/category`,
      payload,
    );
  }
}
