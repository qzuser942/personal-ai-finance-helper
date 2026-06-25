/**
 * 用户登录页面
 * 支持：账号密码登录、记住密码、JWT自动缓存
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { AuthApi } from "../../network/api/authApi";
import { AppState } from "../../store/AppState";
import { StorageHelper, KEYS } from "../../utils/storage";
import { ToastUtil } from "../../utils/toast";

@Entry
@Component
struct LoginPage {
  @State username: string = "";
  @State password: string = "";
  @State isLoading: boolean = false;
  @State rememberMe: boolean = false;
  @State isOffline: boolean = false;

  async aboutToAppear(): Promise<void> {
    // 加载记住的登录信息
    const savedUser = await StorageHelper.getRememberUsername();
    const savedPwd = await StorageHelper.getRememberPassword();
    const savedRemember = await StorageHelper.getRememberMe();
    if (savedRemember) {
      this.username = savedUser;
      this.password = savedPwd;
      this.rememberMe = true;
    }
    // 检查是否已登录
    await AppState.loadLoginState();
    if (AppState.isLoggedIn) {
      // P2-4 修复：启动时如已登录，先做 token 有效性预校验，避免跳转后立刻被踢回
      // 复用 AuthApi.getProfile()（后端 /api/user/profile 端点）
      try {
        const res = await AuthApi.getProfile();
        if (res.code === 200) {
          const passcodeEnabled = await StorageHelper.getPasscodeEnabled();
          if (passcodeEnabled) {
            router.replaceUrl({ url: "pages/auth/LockPage" });
          } else {
            router.replaceUrl({ url: "pages/index/MainPage" });
          }
        } else {
          // token 已失效，清除登录态，停在登录页
          await AppState.logout();
          ToastUtil.showWarning("登录已过期，请重新登录");
        }
      } catch (err) {
        // 网络异常保守处理：停留在登录页，不自动跳转
        console.error("启动token预校验失败:", JSON.stringify(err));
      }
    }
  }

  async onLogin(): Promise<void> {
    if (!this.username.trim()) {
      ToastUtil.showError("请输入用户名");
      return;
    }
    if (!this.password) {
      ToastUtil.showError("请输入密码");
      return;
    }

    this.isLoading = true;
    try {
      const res = await AuthApi.login({
        username: this.username.trim(),
        password: this.password,
      });

      if (res.code === 200 && res.data) {
        await AppState.saveLogin(res.data.token, res.data.userId, res.data.username);
        // 记住密码
        if (this.rememberMe) {
          await StorageHelper.saveRememberLogin(this.username, this.password);
          await StorageHelper.putBoolean(KEYS.REMEMBER_ME, true);
        } else {
          await StorageHelper.putBoolean(KEYS.REMEMBER_ME, false);
          await StorageHelper.remove(KEYS.REMEMBER_PASSWORD);
        }
        ToastUtil.showSuccess("登录成功");
        const passcodeEnabled = await StorageHelper.getPasscodeEnabled();
        if (passcodeEnabled) {
          router.replaceUrl({ url: "pages/auth/LockPage" });
        } else {
          router.replaceUrl({ url: "pages/index/MainPage" });
        }
      } else {
        ToastUtil.showError(res.message || "登录失败");
        if (res.code === -1) {
          this.isOffline = true;
          ToastUtil.showWarning("网络不可达，请检查连接后重试");
        }
      }
    } catch (err) {
      ToastUtil.showError("登录异常，请重试");
    } finally {
      this.isLoading = false;
    }
  }

  build() {
    Column() {
      // Logo区域
      Column() {
        Text("💰")
          .fontSize(60)
          .margin({ bottom: 12 })
        Text("智能理财")
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
        Text("个人智能理财助手")
          .fontSize(14)
          .fontColor($r("app.color.text_hint"))
          .margin({ top: 6 })
      }
      .width("100%")
      .padding({ top: 80, bottom: 40 })
      .alignItems(HorizontalAlign.Center)

      // 登录表单
      Column() {
        // 用户名输入
        TextInput({ placeholder: "请输入用户名", text: this.username })
          .width("100%")
          .height(48)
          .fontSize(15)
          .backgroundColor($r("app.color.bg_page"))
          .borderRadius(8)
          .placeholderColor($r("app.color.text_hint"))
          .onChange((value: string) => {
            this.username = value;
          })

        // 密码输入
        TextInput({ placeholder: "请输入密码", text: this.password })
          .width("100%")
          .height(48)
          .fontSize(15)
          .type(InputType.Password)
          .backgroundColor($r("app.color.bg_page"))
          .borderRadius(8)
          .placeholderColor($r("app.color.text_hint"))
          .margin({ top: 16 })
          .onChange((value: string) => {
            this.password = value;
          })

        // 记住密码
        Row() {
          Checkbox({ name: "rememberMe", group: "rememberGroup" })
            .select(this.rememberMe)
            .selectedColor($r("app.color.primary"))
            .onChange((value: boolean) => {
              this.rememberMe = value;
            })
          Text("记住密码")
            .fontSize(13)
            .fontColor($r("app.color.text_hint"))
            .margin({ left: 4 })
        }
        .width("100%")
        .margin({ top: 12 })

        // 登录按钮
        Button(this.isLoading ? "登录中..." : "登 录")
          .width("100%")
          .height(48)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .backgroundColor($r("app.color.primary"))
          .borderRadius(24)
          .enabled(!this.isLoading)
          .margin({ top: 24 })
          .onClick(() => {
            this.onLogin();
          })

        // 注册入口
        Row() {
          Text("还没有账号？")
            .fontSize(13)
            .fontColor($r("app.color.text_hint"))
          Text("立即注册")
            .fontSize(13)
            .fontColor($r("app.color.primary"))
            .margin({ left: 4 })
            .onClick(() => {
              router.pushUrl({ url: "pages/auth/RegisterPage" });
            })
        }
        .margin({ top: 24 })
      }
      .width("85%")
      .alignItems(HorizontalAlign.Center)

      // 底部版权信息
      Text("胡宪棋 | 软件2413 | 202421332084 | v1.0")
        .fontSize(11)
        .fontColor($r("app.color.text_hint"))
        .margin({ top: 40, bottom: 20 })
    }
    .width("100%")
    .height("100%")
    .backgroundColor($r("app.color.bg_card"));
  }
}
