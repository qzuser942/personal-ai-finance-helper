/**
 * 全局应用状态管理
 * 管理：登录态、主题模式、网络状态、Token
 *
 * 主题架构（完整修复版）：
 * - static themeMode 作为唯一状态源，与 AppStorage('themeMode') 保持同步
 * - PersistentStorage 自动将 themeMode 持久化，StorageHelper 作为双重保险
 * - 任何页面通过 @StorageLink('themeMode') 即可自动订阅主题变化并触发重渲染
 * - setColorMode() 在状态变更时**同步**调用，不被 I/O 阻塞
 * - setThemeMode / toggleTheme 均为同步方法（返回 void / 'light'|'dark'）
 *
 * @author 胡宪棋 软件2413 202421332084
 */
import ConfigurationConstant from "@ohos.app.ability.ConfigurationConstant";
import common from "@ohos.app.ability.common";
import { StorageHelper, KEYS } from "../utils/storage";

const THEME_KEY = "themeMode";
const DEFAULT_THEME: "light" | "dark" = "light";

export class AppState {
  static context: Context;
  static token: string = "";
  static userId: number = 0;
  static username: string = "";
  static isLoggedIn: boolean = false;
  static isOnline: boolean = true;
  static BASE_URL: string = "http://192.168.159.1:8080";
  static offlineCount: number = 0;

  /**
   * 静态主题状态字段（唯一状态源）
   * 与 AppStorage('themeMode') 保持双向同步：
   *   - 写入时：先更新此字段，再同步到 AppStorage
   *   - 读取时：优先从此字段，备选 AppStorage
   */
  static themeMode: "light" | "dark" = DEFAULT_THEME;

  /**
   * 初始化全局状态（在 EntryAbility.onCreate 中调用）
   * 顺序：
   *   1) PersistentStorage 关联持久化键（自动从磁盘恢复）
   *   2) 从 AppStorage 读取已恢复的主题值，同步到 static themeMode
   *   3) 同步应用主题（setColorMode）
   */
  static init(context: Context): void {
    AppState.context = context;

    // 1. 注册 PersistentStorage 关联（第一次调用时用 DEFAULT_THEME，重启后自动恢复）
    try {
      PersistentStorage.persistProp(THEME_KEY, DEFAULT_THEME);
    } catch (e) {
      console.error("PersistentStorage.persistProp 失败:", JSON.stringify(e));
    }

    // 2. 从 AppStorage 读取已恢复值（PersistentStorage 已在上一步写入）
    const saved = AppStorage.get<string>(THEME_KEY);
    if (saved === "light" || saved === "dark") {
      AppState.themeMode = saved;
    } else {
      // 确保 AppStorage 中有合法值
      AppStorage.setOrCreate(THEME_KEY, DEFAULT_THEME);
      AppState.themeMode = DEFAULT_THEME;
    }

    // 3. 同步应用到系统 ColorMode
    AppState.applyThemeMode();
  }

  /**
   * 获取当前主题模式（从 static themeMode 读取，与 AppStorage 同步）
   */
  static getThemeMode(): "light" | "dark" {
    return AppState.themeMode;
  }

  /**
   * 同步切换主题（核心修复）
   * 1. 更新 static themeMode（测试可直接访问）
   * 2. 同步更新 AppStorage（所有 @StorageLink 页面立即响应）
   * 3. 同步调用 setColorMode（系统资源立即切换）
   * 4. 异步写 StorageHelper 作为双重持久化保险（不阻塞 UI）
   * 注意：方法本身为同步，返回 void，保证 UI 调用不阻塞
   */
  static setThemeMode(mode: "light" | "dark"): void {
    if (mode !== "light" && mode !== "dark") {
      console.error(`setThemeMode: 非法输入 ${mode}，已忽略`);
      return;
    }
    // 同步更新静态字段（测试钩子 & 本类内部读取均依赖此）
    AppState.themeMode = mode;
    // 同步更新 AppStorage（驱动所有 @StorageLink('themeMode') 订阅者重渲染）
    AppStorage.setOrCreate(THEME_KEY, mode);
    // 同步应用到系统 ColorMode
    AppState.applyThemeMode();
    // 异步双写 StorageHelper（Preferences），不阻塞 UI
    AppState.persistThemeAsync(mode);
  }

  /**
   * 切换主题（同步，供 UI 直接调用，返回新的主题值）
   */
  static toggleTheme(): "light" | "dark" {
    const next: "light" | "dark" = AppState.themeMode === "light" ? "dark" : "light";
    AppState.setThemeMode(next);
    return next;
  }

  /**
   * 将 themeMode 异步写入 StorageHelper（双重持久化保险）
   * 不阻塞 UI，在后台默默完成
   */
  private static persistThemeAsync(mode: string): void {
    StorageHelper.saveThemeMode(mode).catch((err: Error) => {
      console.warn("StorageHelper.saveThemeMode 异步写入失败（已忽略）:", JSON.stringify(err));
    });
  }

  /**
   * 将 AppStorage 中的 themeMode 同步应用到 applicationContext + mainWindow
   * 同步调用，不被任何 async I/O 阻塞。
   */
  static applyThemeMode(): void {
    const mode = AppState.themeMode;
    const colorMode =
      mode === "dark"
        ? ConfigurationConstant.ColorMode.COLOR_MODE_DARK
        : ConfigurationConstant.ColorMode.COLOR_MODE_LIGHT;

    try {
      if (AppState.context) {
        const appCtx = AppState.context.getApplicationContext();
        appCtx.setColorMode(colorMode);
      }
    } catch (err) {
      console.error("applicationContext.setColorMode 失败:", JSON.stringify(err));
    }
  }

  /**
   * 加载本地存储的登录状态（在 EntryAbility.onCreate 末尾 await 调用）
   * 主题已由 init() 通过 PersistentStorage 自动加载，此方法只处理 token。
   * 同时从 StorageHelper 再次校验主题（双重保险）。
   */
  static async loadLoginState(): Promise<void> {
    try {
      const token = await StorageHelper.getToken();
      if (token) {
        AppState.token = token;
        AppState.isLoggedIn = true;
      }
    } catch (err) {
      console.error("加载登录状态失败:", JSON.stringify(err));
    }

    // 从 Preferences 再次校验主题（双重保险，处理 PersistentStorage 初始化顺序问题）
    try {
      const savedTheme = await StorageHelper.getThemeMode();
      if ((savedTheme === "light" || savedTheme === "dark") && savedTheme !== AppState.themeMode) {
        // Preferences 中的值与当前不一致，以 Preferences 为准
        AppState.setThemeMode(savedTheme as "light" | "dark");
      }
    } catch (err) {
      console.warn("从 StorageHelper 加载主题失败（已忽略）:", JSON.stringify(err));
    }
  }

  static async saveLogin(token: string, userId: number, username: string): Promise<void> {
    AppState.token = token;
    AppState.userId = userId;
    AppState.username = username;
    AppState.isLoggedIn = true;
    await StorageHelper.saveToken(token);
  }

  static async logout(): Promise<void> {
    AppState.token = "";
    AppState.userId = 0;
    AppState.username = "";
    AppState.isLoggedIn = false;
    await StorageHelper.clearLoginData();
    // 退出登录后重置为默认浅色主题
    AppState.setThemeMode(DEFAULT_THEME);
  }

  static getAuthHeader(): string {
    return `Bearer ${AppState.token}`;
  }
}
