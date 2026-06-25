/**
 * 同步账单接口
 * @author 胡宪棋 软件2413 202421332084
 */

/** 离线同步账单项 */
export interface SyncBillItem {
  uuid: string;
  amount: number;
  type: string;
  categoryId: number;
  categoryName: string;
  remark: string;
  consumeTime: string;
  createdAt: string;
  hasImage: boolean;
  receiptImage: string;
}
