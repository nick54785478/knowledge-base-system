// 對齊後端的 PagedQueriedView
export interface PagedQueriedView<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}
