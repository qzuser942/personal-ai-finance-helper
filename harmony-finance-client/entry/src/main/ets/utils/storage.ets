/**
 * Preferences统一缓存管理
 * 仅缓存：JWT Token、离线待同步账单、主题配置、密码锁、草稿账单
 * 绝对禁止任何本地RDB数据库依赖
 * @author 胡宪棋 软件2413 202421332084
 */
import preferences from "@ohos.data.preferences";

const STORE_NAME = "finance_app_prefs";

/** Preferences Key 接口 */
export interface PrefKeys {
  AUTH_TOKEN: string;
  OFFLINE_BILL_QUEUE: string;
  DRAFT_BILL: string;
  THEME_MODE: string;
  PASSCODE_ENABLED: string;
  PASSCODE_VALUE: string;
  REMEMBER_USERNAME: string;
  REMEMBER_PASSWORD: string;
  REMEMBER_ME: string;
}

/** Preferences存储Key定义（对照SRS 6.1.5节） */
export const KEYS: PrefKeys = {
  AUTH_TOKEN: "auth_token",
  OFFLINE_BILL_QUEUE: "offline_bill_queue",
  DRAFT_BILL: "draft_bill",
  THEME_MODE: "theme_mode",
  PASSCODE_ENABLED: "passcode_enabled",
  PASSCODE_VALUE: "passcode_value",
  REMEMBER_USERNAME: "remember_username",
  REMEMBER_PASSWORD: "remember_password",
  REMEMBER_ME: "remember_me",
};

export class StorageHelper {
  private static prefs: preferences.Preferences;

  /** 初始化（在EntryAbility.onCreate中调用） */
  static async init(context: Context): Promise<void> {
    try {
      StorageHelper.prefs = await preferences.getPreferences(context, STORE_NAME);
    } catch (err) {
      console.error("Preferences初始化失败:", JSON.stringify(err));
    }
  }

  /** 保存字符串 */
  static async putString(key: string, value: string): Promise<void> {
    try {
      await StorageHelper.prefs.put(key, value);
      await StorageHelper.prefs.flush();
    } catch (err) {
      console.error(`Preferences putString ${key} 失败:`, JSON.stringify(err));
    }
  }

  /** 获取字符串 */
  static async getString(key: string, defaultValue: string = ""): Promise<string> {
    try {
      return (await StorageHelper.prefs.get(key, defaultValue)) as string;
    } catch (err) {
      return defaultValue;
    }
  }

  /** 保存布尔值 */
  static async putBoolean(key: string, value: boolean): Promise<void> {
    try {
      await StorageHelper.prefs.put(key, value);
      await StorageHelper.prefs.flush();
    } catch (err) {
      console.error(`Preferences putBoolean ${key} 失败:`, JSON.stringify(err));
    }
  }

  /** 获取布尔值 */
  static async getBoolean(key: string, defaultValue: boolean = false): Promise<boolean> {
    try {
      return (await StorageHelper.prefs.get(key, defaultValue)) as boolean;
    } catch (err) {
      return defaultValue;
    }
  }

  /** 删除某项 */
  static async remove(key: string): Promise<void> {
    try {
      await StorageHelper.prefs.delete(key);
      await StorageHelper.prefs.flush();
    } catch (err) {
      console.error(`Preferences remove ${key} 失败:`, JSON.stringify(err));
    }
  }

  /** 清空所有（退出登录时调用） */
  static async clearAll(): Promise<void> {
    try {
      await StorageHelper.prefs.clear();
      await StorageHelper.prefs.flush();
    } catch (err) {
      console.error("Preferences clearAll 失败:", JSON.stringify(err));
    }
  }

  /** 清除登录相关数据（token、记住密码、草稿、离线账单），保留密码锁和主题 */
  static async clearLoginData(): Promise<void> {
    try {
      await StorageHelper.prefs.delete(KEYS.AUTH_TOKEN);
      await StorageHelper.prefs.delete(KEYS.REMEMBER_USERNAME);
      await StorageHelper.prefs.delete(KEYS.REMEMBER_PASSWORD);
      await StorageHelper.prefs.delete(KEYS.REMEMBER_ME);
      await StorageHelper.prefs.delete(KEYS.OFFLINE_BILL_QUEUE);
      await StorageHelper.prefs.delete(KEYS.DRAFT_BILL);
      await StorageHelper.prefs.flush();
    } catch (err) {
      console.error("Preferences clearLoginData 失败:", JSON.stringify(err));
    }
  }

  // ==================== 业务便捷方法 ====================

  /** 获取JWT Token */
  static async getToken(): Promise<string> {
    return StorageHelper.getString(KEYS.AUTH_TOKEN, "");
  }

  /** 保存JWT Token */
  static async saveToken(token: string): Promise<void> {
    await StorageHelper.putString(KEYS.AUTH_TOKEN, token);
  }

  /** 获取离线账单队列 */
  static async getOfflineBills(): Promise<string> {
    return StorageHelper.getString(KEYS.OFFLINE_BILL_QUEUE, "[]");
  }

  /** 保存离线账单队列 */
  static async saveOfflineBills(billsJson: string): Promise<void> {
    await StorageHelper.putString(KEYS.OFFLINE_BILL_QUEUE, billsJson);
  }

  /** 获取主题模式 */
  static async getThemeMode(): Promise<string> {
    return StorageHelper.getString(KEYS.THEME_MODE, "light");
  }

  /** 保存主题模式 */
  static async saveThemeMode(mode: string): Promise<void> {
    await StorageHelper.putString(KEYS.THEME_MODE, mode);
  }

  /** 获取密码锁开关状态 */
  static async getPasscodeEnabled(): Promise<boolean> {
    return StorageHelper.getBoolean(KEYS.PASSCODE_ENABLED, false);
  }

  /** 获取密码锁密码 */
  static async getPasscodeValue(): Promise<string> {
    return StorageHelper.getString(KEYS.PASSCODE_VALUE, "");
  }

  /** 保存草稿 */
  static async saveDraft(draftJson: string): Promise<void> {
    await StorageHelper.putString(KEYS.DRAFT_BILL, draftJson);
  }

  /** 获取草稿 */
  static async getDraft(): Promise<string> {
    return StorageHelper.getString(KEYS.DRAFT_BILL, "");
  }

  /** 清除草稿 */
  static async clearDraft(): Promise<void> {
    await StorageHelper.remove(KEYS.DRAFT_BILL);
  }

  /** 获取记住的用户名 */
  static async getRememberUsername(): Promise<string> {
    return StorageHelper.getString(KEYS.REMEMBER_USERNAME, "");
  }

  /** 获取记住的密码 */
  static async getRememberPassword(): Promise<string> {
    return StorageHelper.getString(KEYS.REMEMBER_PASSWORD, "");
  }

  /** 获取记住密码开关 */
  static async getRememberMe(): Promise<boolean> {
    return StorageHelper.getBoolean(KEYS.REMEMBER_ME, false);
  }

  /** 保存记住的登录信息 */
  static async saveRememberLogin(username: string, password: string): Promise<void> {
    await StorageHelper.putString(KEYS.REMEMBER_USERNAME, username);
    await StorageHelper.putString(KEYS.REMEMBER_PASSWORD, password);
  }
}
