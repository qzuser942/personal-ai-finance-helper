/**
 * 日期时间工具类
 * @author 胡宪棋 软件2413 202421332084
 */

export class DateUtil {
  /**
   * 格式化日期为 yyyy-MM-dd
   */
  static formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  /**
   * 格式化日期时间为 yyyy-MM-dd HH:mm:ss
   */
  static formatDateTime(date: Date): string {
    const yyyyMMdd = DateUtil.formatDate(date);
    const h = String(date.getHours()).padStart(2, '0');
    const min = String(date.getMinutes()).padStart(2, '0');
    const s = String(date.getSeconds()).padStart(2, '0');
    return `${yyyyMMdd} ${h}:${min}:${s}`;
  }

  /**
   * 获取当前年月 yyyy-MM
   */
  static getCurrentYearMonth(): string {
    const now = new Date();
    const y = now.getFullYear();
    const m = String(now.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
  }

  /**
   * 获取当前完整日期时间字符串
   */
  static getCurrentDateTimeStr(): string {
    return DateUtil.formatDateTime(new Date());
  }

  /**
   * 获取当前日期字符串
   */
  static getCurrentDateStr(): string {
    return DateUtil.formatDate(new Date());
  }

  /**
   * 格式化金额显示（¥1,234.56）
   */
  static formatMoney(amount: number): string {
    if (amount === null || amount === undefined) return '¥0.00';
    const fixed = amount.toFixed(2);
    const parts = fixed.split('.');
    const intPart = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return `¥${intPart}.${parts[1]}`;
  }

  /**
   * 获取月份第一天
   */
  static getFirstDayOfMonth(yearMonth: string): string {
    return `${yearMonth}-01`;
  }

  /**
   * 月份加减
   */
  static addMonths(yearMonth: string, months: number): string {
    const [y, m] = yearMonth.split('-').map(Number);
    const date = new Date(y, m - 1 + months, 1);
    return DateUtil.getYearMonth(date);
  }

  /**
   * Date转yearMonth字符串
   */
  static getYearMonth(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    return `${y}-${m}`;
  }

  /**
   * 生成UUID v4 (用于离线账单去重)
   */
  static generateUUID(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
      const r = Math.random() * 16 | 0;
      const v = c === 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
  }

  /**
   * 获取本月的天数
   */
  static getDaysInMonth(yearMonth: string): number {
    const [y, m] = yearMonth.split('-').map(Number);
    return new Date(y, m, 0).getDate();
  }

  /**
   * 月份中文显示
   */
  static formatYearMonthCN(yearMonth: string): string {
    const [y, m] = yearMonth.split('-');
    return `${y}年${parseInt(m)}月`;
  }
}
