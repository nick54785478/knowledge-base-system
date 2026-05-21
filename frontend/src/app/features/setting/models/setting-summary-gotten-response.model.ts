import { SettingQueried } from './setting-queried.model';

export class SettingSummaryGottenResource {
  code!: string;
  message!: string;
  data: SettingQueried[] = [];
}
