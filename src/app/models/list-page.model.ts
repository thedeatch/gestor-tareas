export interface ListPage<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  allTotal: number;
}
