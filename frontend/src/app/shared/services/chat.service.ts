import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private apiUrl = 'http://localhost:8080/api/v1/assistant/ask-stream';
  private readonly baseUrl = `${environment.apiEndpoint}/chat`;

  // 注入 NgZone 確保 EventSource 的回呼能觸發 Angular 畫面更新
  constructor(private zone: NgZone) {}

  askQuestionStream(question: string): Observable<string> {
    const apiUrl = this.baseUrl + '/assistant/ask-stream';
    return new Observable<string>((observer) => {
      // 組合 API URL (GET 請求)
      const url = `${this.apiUrl}?q=${encodeURIComponent(question)}`;
      const eventSource = new EventSource(url);

      // 監聽伺服器傳來的字串片段
      eventSource.onmessage = (event) => {
        this.zone.run(() => {
          // LLM 傳回的通常是純文字片段，直接發送給訂閱者
          observer.next(event.data);
        });
      };

      // 錯誤處理或串流結束
      eventSource.onerror = (error) => {
        this.zone.run(() => {
          // 當伺服器主動關閉連線，或是發生錯誤時，關閉 EventSource 並結束 Observable
          eventSource.close();
          observer.complete();
        });
      };

      // 當元件銷毀或取消訂閱時，主動斷開連線
      return () => eventSource.close();
    });
  }
}
