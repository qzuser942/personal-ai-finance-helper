/**
 * 统一消息提示工具
 * @author 胡宪棋 软件2413 202421332084
 */
import promptAction from '@ohos.promptAction';

export class ToastUtil {
  /** 成功提示 */
  static showSuccess(message: string): void {
    promptAction.showToast({
      message: message,
      duration: 2000,
      bottom: '50%'
    });
  }

  /** 错误提示 */
  static showError(message: string): void {
    promptAction.showToast({
      message: message,
      duration: 2500,
      bottom: '50%'
    });
  }

  /** 普通信息提示 */
  static showInfo(message: string): void {
    promptAction.showToast({
      message: message,
      duration: 2000,
      bottom: '50%'
    });
  }

  /** 警告提示 */
  static showWarning(message: string): void {
    promptAction.showToast({
      message: `⚠ ${message}`,
      duration: 3000,
      bottom: '50%'
    });
  }
}
