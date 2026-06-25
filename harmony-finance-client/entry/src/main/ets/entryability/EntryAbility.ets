/**
 * 应用入口Ability
 * 个人智能理财系统 - 鸿蒙客户端
 *
 * 主题初始化时序（关键）：
 *   onCreate:
 *     1. StorageHelper.init()      → Preferences 就绪
 *     2. AppState.init()           → PersistentStorage 注册 + 读取已持久化主题 + 同步 setColorMode
 *     3. AppState.loadLoginState() → 加载 token + 从 Preferences 二次校验主题
 *   onWindowStageCreate:
 *     4. windowStage.loadContent() → 加载页面（主题已在第 2 步同步生效）
 *
 * 确保主题在第一帧渲染前已通过 applicationContext.setColorMode 生效。
 *
 * @author 胡宪棋 软件2413 202421332084
 */
import UIAbility from "@ohos.app.ability.UIAbility";
import AbilityConstant from "@ohos.app.ability.AbilityConstant";
import Want from "@ohos.app.ability.Want";
import window from "@ohos.window";
import { AppState } from "../store/AppState";
import { StorageHelper } from "../utils/storage";
import { AuthApi } from "../network/api/authApi";

export default class EntryAbility extends UIAbility {
  async onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): Promise<void> {
    // Step 1: Preferences 初始化（必须先于 AppState.init，因为后者可能读 StorageHelper）
    await StorageHelper.init(this.context);

    // Step 2: 全局状态初始化（PersistentStorage 注册 + 读取已持久化主题 + 同步 setColorMode）
    AppState.init(this.context);

    // Step 3: 加载登录态 + 从 Preferences 二次校验主题（处理 PersistentStorage 初始化顺序边缘情况）
    await AppState.loadLoginState();
  }

  async onWindowStageCreate(windowStage: window.WindowStage) {
    // Step 4: 登录态 → 密码锁 → 页面路由（优先级：登录 > 密码锁 > 主页）
    let pagePath: string;
    if (!AppState.isLoggedIn) {
      pagePath = "pages/auth/LoginPage";
    } else {
      // 已登录时预校验token有效性，防止过期token绕过登录页
      let tokenValid = false;
      try {
        const profileRes = await AuthApi.getProfile();
        tokenValid = profileRes.code === 200;
      } catch (e) {
        console.info("EntryAbility: token预校验网络异常");
      }

      if (!tokenValid) {
        await AppState.logout();
        pagePath = "pages/auth/LoginPage";
      } else {
        const passcodeEnabled = await StorageHelper.getPasscodeEnabled();
        pagePath = passcodeEnabled ? "pages/auth/LockPage" : "pages/index/MainPage";
      }
    }

    windowStage.loadContent(pagePath, (err) => {
      if (err.code) {
        console.error("loadContent 失败:", JSON.stringify(err));
        return;
      }
    });

    // 设置窗口全屏布局
    windowStage.getMainWindow((err, win) => {
      if (err.code) {
        console.error("getMainWindow 失败:", JSON.stringify(err));
        return;
      }
      win.setWindowLayoutFullScreen(true);
    });
  }

  onWindowStageDestroy() {}

  onForeground() {}

  onBackground() {}
}
