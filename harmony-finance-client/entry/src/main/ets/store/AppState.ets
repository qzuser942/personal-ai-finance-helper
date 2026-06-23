/**
 * 全局应用状态管理
 * 管理：登录态、主题模式、网络状态、Token
 * @author 胡宪棋 软件2413 202421332084
 */
import { StorageHelper, KEYS } from '../utils/storage';

export class AppState {
  /** 全局Context */
  static context: Context;

  /** 当前登录Token */
  static token: string = '';

  /** 当前登录用户ID */
  static userId: number = 0;

  /** 当前登录用户名 */
  static username: string = '';

  /** 当前主题模式 */
  static themeMode: 'light' | 'dark' = 'light';

  /** 是否已登录 */
  static isLoggedIn: boolean = false;

  /** 网络是否在线 */
  static isOnline: boolean = true;

  /** 服务端基础URL（修改此处切换后端地址） */
  static BASE_URL: string = 'http://192.168.1.100:8080';

  /** 离线待同步数量 */
  static offlineCount: number = 0;

  /** 初始化 */
  static init(context: Context): void {
    AppState.context = context;
  }

  /** 加载本地存储的登录状态 */
  static async loadLoginState(): Promise<void> {
    try {
      const token = await StorageHelper.getToken();
      if (token) {
        AppState.token = token;
        AppState.isLoggedIn = true;
      }
      const theme = await StorageHelper.getThemeMode();
      AppState.themeMode = theme === 'dark' ? 'dark' : 'light';
    } catch (err) {
      console.error('加载登录状态失败:', JSON.stringify(err));
    }
  }

  /** 保存登录信息 */
  static async saveLogin(token: string, userId: number, username: string): Promise<void> {
    AppState.token = token;
    AppState.userId = userId;
    AppState.username = username;
    AppState.isLoggedIn = true;
    await StorageHelper.saveToken(token);
  }

  /** 退出登录 */
  static async logout(): Promise<void> {
    AppState.token = '';
    AppState.userId = 0;
    AppState.username = '';
    AppState.isLoggedIn = false;
    await StorageHelper.clearAll();
  }

  /** 切换主题 */
  static async toggleTheme(): Promise<string> {
    AppState.themeMode = AppState.themeMode === 'light' ? 'dark' : 'light';
    await StorageHelper.saveThemeMode(AppState.themeMode);
    return AppState.themeMode;
  }

  /** 设置主题 */
  static async setThemeMode(mode: 'light' | 'dark'): Promise<void> {
    AppState.themeMode = mode;
    await StorageHelper.saveThemeMode(mode);
  }

  /** 获取Authorization Header */
  static getAuthHeader(): string {
    return `Bearer ${AppState.token}`;
  }
}
