/**
 * 输入校验工具类
 * @author 胡宪棋 软件2413 202421332084
 */

export class ValidationUtil {
  /**
   * 检查输入是否包含非数字字符（仅允许0-9和小数点）
   * @param value 原始输入
   * @returns 是否包含非法字符
   */
  static hasNonNumericChar(value: string): boolean {
    for (let i = 0; i < value.length; i++) {
      const ch = value.charAt(i);
      if (!(ch >= "0" && ch <= "9") && ch !== ".") {
        return true;
      }
    }
    return false;
  }

  /**
   * 过滤金额输入，只允许数字和一个小数点
   * @param value 原始输入
   * @returns 过滤后的合法字符串
   */
  static filterAmountInput(value: string): string {
    let result = "";
    let hasDot = false;
    for (let i = 0; i < value.length; i++) {
      const ch = value.charAt(i);
      if (ch >= "0" && ch <= "9") {
        result += ch;
      } else if (ch === "." && !hasDot) {
        hasDot = true;
        result += ch;
      }
    }
    return result;
  }

  /**
   * 校验金额输入
   * 规则：正数、最多8位整数、最多2位小数
   * @param value 输入字符串
   * @returns 错误提示文本，合法返回空字符串
   */
  static validateAmount(value: string): string {
    if (!value || value.trim() === "") {
      return "";
    }

    const trimmed = value.trim();

    // 以小数点开头或结尾，暂不校验，等用户输完
    if (trimmed === "." || trimmed.endsWith(".")) {
      return "";
    }

    // 检查是否为有效数字格式（至少一位整数，可选小数部分）
    if (!/^\d+(\.\d*)?$/.test(trimmed)) {
      return "⚠️警告：请输入有效的正数金额";
    }

    const num = parseFloat(trimmed);

    // 金额必须大于0
    if (num <= 0) {
      return "⚠️警告：金额必须大于0";
    }

    // 整数部分最多8位
    const parts = trimmed.split(".");
    if (parts[0].length > 8) {
      return "⚠️警告：整数部分最多8位";
    }

    // 小数部分最多2位
    if (parts.length > 1 && parts[1].length > 2) {
      return "⚠️警告：小数部分最多2位";
    }

    return "";
  }
}
