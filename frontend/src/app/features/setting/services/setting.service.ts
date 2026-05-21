import { Injectable } from '@angular/core';
import { UpdateSetting } from '../models/update-setting-request.model';
import { Observable } from 'rxjs/internal/Observable';
import { CreateSettingResource } from '../models/create-setting-request.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { SettingQueried } from '../models/setting-queried.model';
import { SettingSummaryGottenResource } from '../models/setting-summary-gotten-response.model';
import { BaseResponse } from '../../../shared/models/base-response.model';
import { map } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class SettingService {
  private readonly baseApiUrl = environment.apiEndpoint;

  constructor(private http: HttpClient) {}

  /**
   * 新增一筆 Setting 資料
   * @param request
   * @return Observable<any>
   */
  create(request: CreateSettingResource): Observable<any> {
    const url = this.baseApiUrl + '/settings';
    return this.http.post(url, request);
  }

  /**
   * 更新一筆資料
   * @param request
   * @return Observable<any>
   */
  update(id: number, request: UpdateSetting): Observable<any> {
    const url = this.baseApiUrl + '/settings/' + id;
    return this.http.put(url, request);
  }

  /**
   * 查詢設定總覽
   * @param dataType 資料種類
   * @param type 種類
   * @param name 名稱
   */
  summary(
    dataType: string,
    type: string,
    name: string,
    activeFlag: string,
  ): Observable<SettingQueried[]> {
    const url = this.baseApiUrl + '/settings/summary';
    let params = new HttpParams()
      .set('dataType', dataType ? dataType : '')
      .set('type', type ? type : '')
      .set('name', name ? name : '')
      .set('activeFlag', activeFlag ? activeFlag : '');
    return this.http.get<SettingSummaryGottenResource>(url, { params }).pipe(
      map((res) => {
        return res.data;
      }),
    );
  }

  /**
   * 刪除
   *
   * @param id
   */
  delete(id: number): Observable<BaseResponse> {
    const url = this.baseApiUrl + '/settings/' + id;
    return this.http.delete<BaseResponse>(url);
  }
}
