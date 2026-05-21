import {
  AfterViewChecked,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { ChatMessage } from '../../models/chat-message-gotten.model';
import { SystemMessageService } from '../../../../shared/services/system-message.service';
import { CommonModule } from '@angular/common';
import { InputTextModule } from 'primeng/inputtext';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { PRIME_COMPONENTS } from '../../../../shared/shared-primeng';
import { AutoCompleteModule } from 'primeng/autocomplete';
import { TextareaModule } from 'primeng/textarea';
/**
 * AI 智能助理元件 (Virgo 企業大腦)
 * * 負責處理與後端 Spring Boot 的 WebSocket 雙向通訊，
 * 實作了串流回覆 (Streaming)、骨架屏思考特效 (Shimmer)，以及使用者輸入管理。
 */
@Component({
  selector: 'app-rag-assistant',
  standalone: true,
  imports: [
    CommonModule,
    AutoCompleteModule,
    PRIME_COMPONENTS,
    InputTextModule,
    TextareaModule,
    FormsModule,
    ButtonModule,
  ],
  templateUrl: './rag-assistant.component.html',
  styleUrl: './rag-assistant.component.scss',
})
export class RagAssistantComponent
  implements OnInit, OnDestroy, AfterViewChecked
{
  // 綁定 HTML 中的對話視窗容器，用於實現自動滾動到底部
  @ViewChild('chatScrollContainer') private chatScrollContainer!: ElementRef;

  // --- 狀態管理 ---

  /** 存放目前對話中所有的訊息紀錄 */
  messages: ChatMessage[] = [];

  /** 綁定使用者當前輸入的問題文字 */
  question: string = '';

  /** 綁定使用者選擇的知識庫過濾標籤，將作為 @RequestParam 傳送給後端 */
  selectedTags: string[] = [];

  /** 控制輸入框與按鈕的鎖定狀態，true 表示 AI 正在思考或生成回覆中 */
  isGenerating: boolean = false;

  // --- WebSocket 配置 ---

  /** WebSocket 連線實體 */
  private ws: WebSocket | null = null;

  /** * WebSocket 連線端點。
   * 使用 window.location.host 動態取得當前網域，配合 Angular Proxy 或 Nginx 設定，
   * 確保本地開發與正式機環境皆能正確連線。
   */
  private wsUrl = `ws://${window.location.host}/api/v1/assistant/ws`;

  constructor(private messageService: SystemMessageService) {}

  /**
   * 元件初始化生命週期
   */
  ngOnInit(): void {
    // 1. 建立與後端的 WebSocket 雙向連線
    this.connectWebSocket();

    // 2. 預先塞入一條系統歡迎訊息，引導使用者操作
    this.messages.push({
      role: 'assistant',
      content:
        '你好！我是 Virgo 企業大腦智能助理。請設定知識庫標籤，並輸入您的問題，我會為您進行 RAG 檢索並解答。',
    });
  }

  /**
   * 元件銷毀生命週期
   * 確保使用者離開當前頁面時，主動釋放 WebSocket 連線資源
   */
  ngOnDestroy(): void {
    this.disconnectWebSocket();
  }

  /**
   * 視圖更新後的生命週期
   * 每次 Angular 完成畫面渲染（例如新增了一條訊息或串流更新了一個字），
   * 就強制將對話視窗滾動到最底部。
   */
  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  /**
   * 初始化並建立 WebSocket 連線，綁定各項事件監聽器
   */
  private connectWebSocket(): void {
    this.ws = new WebSocket(this.wsUrl);

    // 連線成功事件
    this.ws.onopen = () => {
      console.log('[WebSocket] 已成功連線至 AI 助理');
    };

    // 接收訊息事件 (處理 AI 的串流回覆)
    this.ws.onmessage = (event) => {
      const payload = event.data;

      // 1. 攔截結束暗號：當後端 Flux 結束時會發送 [DONE]
      if (payload === '[DONE]') {
        this.isGenerating = false; // 解鎖輸入框、隱藏停止按鈕

        // 找到最後一個 AI 訊息氣泡，關閉其打字機游標動畫
        const lastMsg = this.messages[this.messages.length - 1];
        if (lastMsg && lastMsg.role === 'assistant') {
          lastMsg.isStreaming = false;
        }
        return; // 直接中斷執行，避免將 [DONE] 渲染到畫面上
      }

      // 2. 錯誤處理：若收到後端傳來的 JSON 格式錯誤訊息
      if (payload.startsWith('{"error"')) {
        const err = JSON.parse(payload);
        this.messageService.showError('產生失敗', err.error);
        this.isGenerating = false;
        return;
      }

      // 3. 正常的串流字串拼接邏輯
      const lastMessage = this.messages[this.messages.length - 1];

      // 若最後一則是使用者的訊息（代表 AI 還沒開始回覆），或是目前的 AI 訊息尚未被標記為串流中，
      // 這邊因為我們在 sendMessage 時已經先塞了一個空內容的 assistant 氣泡，
      // 所以通常會直接走 else 分支進行字串拼接。
      if (lastMessage.role === 'user') {
        this.messages.push({
          role: 'assistant',
          content: payload,
          isStreaming: true,
        });
      } else {
        // 將收到的新字元附加到現有內容後，模擬打字機效果
        lastMessage.content += payload;
      }
    };

    // 連線錯誤事件
    this.ws.onerror = (error) => {
      console.error('[WebSocket] 連線發生錯誤', error);
      this.messageService.showError(
        '連線異常',
        '無法連線至 AI 伺服器，請確認後端狀態',
      );
      this.isGenerating = false;
    };

    // 連線關閉事件
    this.ws.onclose = () => {
      console.warn('[WebSocket] 連線已中斷');
      this.isGenerating = false;
      // 實務上可以在這裡實作自動重連 (Reconnect) 機制，例如 setTimeout 延遲重試
    };
  }

  /**
   * 安全地關閉目前的 WebSocket 連線
   */
  private disconnectWebSocket(): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.close();
    }
  }

  /**
   * 處理使用者送出問題的邏輯
   */
  sendMessage(): void {
    // 防呆檢查：若問題為空、純空白，或 AI 正在生成中，則忽略發送請求
    if (!this.question || this.question.trim() === '' || this.isGenerating) {
      return;
    }

    // 檢查 WebSocket 連線狀態，若斷線則嘗試重連並提示使用者
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      this.messageService.showError(
        '連線失敗',
        '尚未連線至伺服器，正在嘗試重新連線...',
      );
      this.connectWebSocket();
      return;
    }

    // 1. 將使用者的問題推入畫面，顯示在對話視窗右側
    this.messages.push({ role: 'user', content: this.question });

    // 2. 【核心機制】立刻推入一個空的 AI 氣泡。
    // 當 content 為空字串且 isStreaming 為 true 時，HTML 模板會將其渲染為「波浪骨架屏 (思考中)」特效。
    this.messages.push({
      role: 'assistant',
      content: '',
      isStreaming: true,
    });

    // 3. 組合發送給後端的 Payload (需與後端 WebSocketHandler 預期的 JSON 結構一致)
    const requestPayload = {
      question: this.question,
      tags: this.selectedTags,
    };

    // 4. 變更狀態為生成中（鎖定輸入框），並透過 WebSocket 發送 JSON 字串
    this.isGenerating = true;
    this.ws.send(JSON.stringify(requestPayload));

    // 5. 清空輸入框，準備迎接下一次提問
    this.question = '';
  }

  /**
   * 中止 AI 生成
   * 透過直接切斷當前 WebSocket 連線來強行中止後端的 Flux 串流，隨後立即重建一條乾淨的新連線。
   */
  stopGenerating(): void {
    this.disconnectWebSocket(); // 切斷連線，通知後端停止生成
    this.isGenerating = false; // 解鎖前端介面

    // 移除最後一個 AI 氣泡的游標或骨架屏特效，將畫面定格
    this.messages[this.messages.length - 1].isStreaming = false;

    this.connectWebSocket(); // 重新建立準備下一次對話的連線
  }

  /**
   * 將對話視窗的卷軸強制滾動至最底部
   * 搭配 ngAfterViewChecked 使用，確保使用者總能看到最新生成的文字
   */
  private scrollToBottom(): void {
    try {
      this.chatScrollContainer.nativeElement.scrollTop =
        this.chatScrollContainer.nativeElement.scrollHeight;
    } catch (err) {
      // 忽略因 DOM 尚未渲染完成而導致的錯誤
    }
  }

  /**
   * 處理文字輸入框的鍵盤事件
   * 實現了現代通訊軟體常見的行為：
   * - 純按 [Enter]：送出訊息
   * - 按 [Shift] + [Enter]：在輸入框內換行
   * * @param event 鍵盤事件物件
   */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault(); // 阻止 textarea 原生的換行行為
      this.sendMessage(); // 觸發發送邏輯
    }
  }
}
