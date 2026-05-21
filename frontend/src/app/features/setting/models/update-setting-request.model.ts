export class UpdateSetting {
  id!: number;
  dataType!: string; // 資料種類
  type!: string; // 種類
  name!: string; // 名稱
  value!: string; // 值
  description!: string; // 敘述
  priorityNo!: number; // 順序號(從 1 開始)
  activeFlag!: string;
}
