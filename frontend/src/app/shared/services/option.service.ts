import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Option } from '../models/option.model';
import { Observable } from 'rxjs/internal/Observable';
import { map } from 'rxjs/internal/operators/map';
import { environment } from '../../../environments/environment';
import { OptionGottenResource } from '../models/option.gotten.resource.model';

@Injectable({
  providedIn: 'root',
})
export class OptionService {
  private readonly baseApiUrl = environment.apiEndpoint;

  constructor(private http: HttpClient) {}

  /**
   * 取得 Data Type 配置資料
   * @return Observable<MenuItem[]
   */
  public getDataTypes(): Observable<Option[]> {
    return this.http.get<Option[]>('/data-type.json').pipe(
      map((response) => {
        return response;
      }),
    );
  }

  /**
   * 透過 DataType 取得特定下拉式選單
   * @param dataType DataType
   * @return  Observable<Option[]>
   */
  public getOptionsByDataType(dataType: string): Observable<Option[]> {
    const url = this.baseApiUrl + '/options/' + dataType;
    return this.http.get<OptionGottenResource>(url).pipe(
      map((response) => {
        return response.data;
      }),
    );
  }

  /**
   * 透過 DataType 取得特定下拉式選單 ( Type 值)
   * @param dataType DataType
   * @return  Observable<Option[]>
   */
  public getSettingTypes(dataType: string): Observable<Option[]> {
    const url = this.baseApiUrl + '/options/' + dataType + '/type';
    return this.http.get<OptionGottenResource>(url).pipe(
      map((response) => {
        return response.data;
      }),
    );
  }

  /**
   * 透過 DataType 及 Type 取得特定下拉式選單
   * @param dataType DataType
   * @param type Type
   * @return  Observable<Option[]>
   */
  public getOptionsByDataTypeAndType(
    dataType: string,
    type: string,
  ): Observable<Option[]> {
    const url = this.baseApiUrl + '/options/' + dataType + '/' + type;
    return this.http.get<OptionGottenResource>(url).pipe(
      map((response) => {
        return response.data;
      }),
    );
  }
}
