import { Option } from './option.model';

/**
 * 下拉選單
 */
export interface OptionGottenResource {
  code: string;
  message: string;
  data: Option[];
}
