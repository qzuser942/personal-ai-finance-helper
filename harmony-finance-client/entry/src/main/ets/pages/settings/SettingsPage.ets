/**
 * 设置与关于页面
 * 功能：深浅色主题切换、密码锁、手动同步、备份、关于信息、退出登录
 *
 * 主题切换修复要点：
 * - @StorageLink('themeMode') 订阅 AppStorage 变化，驱动页面重渲染
 * - @State isDark 是真正驱动 Toggle isOn 的布尔状态（由 themeMode 派生并及时同步）
 * - 切换时：先同步更新 isDark（Toggle 小球立即滑动），再调用 AppState.setThemeMode
 * - onPageShow 时从 AppState.themeMode 同步 isDark，防止跨页面回来后不一致
 *
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { AppState } from "../../store/AppState";
import { StorageHelper, KEYS } from "../../utils/storage";
import { BillApi } from "../../network/api/billApi";
import { HttpRequest } from "../../network/request";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { OfflineBill } from "../../model/BillModel";
import { ConfirmDialog } from "../../components/common/ConfirmDialog";
import { CsvExportUtil } from "../../utils/CsvExportUtil";

@Entry
@Component
struct SettingsPage {
  /**
   * 订阅 AppStorage('themeMode')：当 AppState.setThemeMode() 修改 AppStorage 后，
   * 此变量自动更新，触发整个 build() 重渲染（背景色、文字色等资源文件切换生效）。
   */
  @StorageLink("themeMode") themeMode: string = "light";

  /**
   * isDark 是驱动 Toggle isOn 的独立布尔状态。
   * 原因：Toggle 的 isOn 参数在 ArkTS 中是值传递，无法绑定计算表达式；
   * 必须用 @State 布尔值才能保证小球跟随点击即时滑动。
   * 每次 themeMode 变化时通过 onPageShow / onToggleTheme 同步更新。
   */
  @State isDark: boolean = false;

  @State passcodeEnabled: boolean = false;
  @State offlineCount: number = 0;
  @State showPasscodeSetup: boolean = false;
  @State passcodeInput: string = "";
  @State passcodeConfirm: string = "";
  private isReady: boolean = false;
  private userToggling: boolean = false;

  /**
   * 退出登录确认弹框控制器（@CustomDialog 必须通过 CustomDialogController 调用）
   */
  logoutDialogController: CustomDialogController = new CustomDialogController({
    builder: ConfirmDialog({
      title: "退出登录",
      message: "确认退出登录？",
      onConfirm: () => {
        this.onLogout();
      },
    }),
    autoCancel: true,
    alignment: DialogAlignment.Center,
  });

  async aboutToAppear(): Promise<void> {
    this.isReady = false;
    this.isDark = AppState.getThemeMode() === "dark";
    this.passcodeEnabled = await StorageHelper.getPasscodeEnabled();
    console.info("[SettingsPage] aboutToAppear passcodeEnabled=" + this.passcodeEnabled);
    await this.checkOffline();
    this.isReady = true;
    console.info("[SettingsPage] aboutToAppear 完成, isReady=true");
  }

  aboutToDisappear(): void {
    this.isReady = false;
    this.userToggling = false;
    console.info("[SettingsPage] aboutToDisappear, isReady=false, userToggling=false");
  }

  onPageShow(): void {
    // 每次页面显示时，从 AppState.themeMode 同步 isDark
    // 防止从其他页面返回时 Toggle 状态与实际主题不一致
    const actual = AppState.getThemeMode();
    const shouldBeDark = actual === "dark";
    if (this.isDark !== shouldBeDark) {
      this.isDark = shouldBeDark;
    }
    // themeMode 也保持同步（驱动资源文件刷新）
    if (this.themeMode !== actual) {
      this.themeMode = actual;
    }
  }

  async checkOffline(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      this.offlineCount = bills.filter((b) => b.syncStatus === "pending").length;
    } catch (e) {
      this.offlineCount = 0;
    }
  }

  /**
   * 主题切换处理（完整修复）
   * 执行顺序：
   *   1. 同步更新 @State isDark → Toggle 小球立即滑动（UI 响应无延迟）
   *   2. 同步调用 AppState.setThemeMode →
   *      a. 更新 AppState.themeMode（静态字段）
   *      b. 更新 AppStorage('themeMode')（驱动 @StorageLink 订阅者重渲染）
   *      c. 调用 setColorMode（系统资源 light/dark 资源文件立即切换）
   *      d. 异步写 StorageHelper Preferences（持久化）
   *   3. 显示正确提示文案
   */
  onToggleTheme(isOn: boolean): void {
    // Step 1: 立即更新 isDark，Toggle 小球即时滑动
    this.isDark = isOn;
    const newMode: "light" | "dark" = isOn ? "dark" : "light";
    // Step 2: 同步更新全局主题状态（触发系统资源切换）
    AppState.setThemeMode(newMode);
    // Step 3: 显示切换提示
    ToastUtil.showSuccess(`已切换为${newMode === "dark" ? "深色" : "浅色"}主题`);
  }

  async onTogglePasscode(enable: boolean): Promise<void> {
    if (!this.isReady) {
      console.info("[SettingsPage] onTogglePasscode 被 isReady 守卫拦截, enable=" + enable);
      return;
    }
    if (!this.userToggling) {
      console.info("[SettingsPage] onTogglePasscode 被 userToggling 守卫拦截, enable=" + enable);
      return;
    }
    this.userToggling = false;

    console.info(
      "[SettingsPage] onTogglePasscode 执行, enable=" +
        enable +
        ", current=" +
        this.passcodeEnabled,
    );

    if (enable) {
      if (!this.passcodeEnabled) {
        this.passcodeEnabled = true;
        this.showPasscodeSetup = true;
      }
    } else {
      if (this.passcodeEnabled) {
        const persistedEnabled = await StorageHelper.getPasscodeEnabled();
        if (!persistedEnabled) {
          console.info("[SettingsPage] 防御校验：Preferences中密码锁已关闭，仅同步UI状态");
          this.passcodeEnabled = false;
          return;
        }
        this.passcodeEnabled = false;
        await StorageHelper.putBoolean(KEYS.PASSCODE_ENABLED, false);
        await StorageHelper.remove(KEYS.PASSCODE_VALUE);
        ToastUtil.showSuccess("密码锁已关闭");
      }
    }
  }

  async onSavePasscode(): Promise<void> {
    if (!this.passcodeInput || this.passcodeInput.length < 4 || this.passcodeInput.length > 6) {
      ToastUtil.showError("请输入4-6位数字密码");
      return;
    }
    if (!/^\d+$/.test(this.passcodeInput)) {
      ToastUtil.showError("密码仅支持数字");
      return;
    }
    if (this.passcodeInput !== this.passcodeConfirm) {
      ToastUtil.showError("两次密码不一致");
      return;
    }

    await StorageHelper.putBoolean(KEYS.PASSCODE_ENABLED, true);
    await StorageHelper.putString(KEYS.PASSCODE_VALUE, this.passcodeInput);
    this.passcodeEnabled = true;
    this.showPasscodeSetup = false;
    this.passcodeInput = "";
    this.passcodeConfirm = "";
    ToastUtil.showSuccess("密码锁已开启");
  }

  async onManualSync(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      const pending = bills.filter((b) => b.syncStatus === "pending");

      if (pending.length === 0) {
        ToastUtil.showInfo("暂无待同步数据");
        return;
      }

      ToastUtil.showInfo(`正在同步${pending.length}条离线账单...`);

      const result = await BillApi.syncOfflineBills(pending);
      if (result) {
        for (const r of result.results) {
          const bill = bills.find((b) => b.uuid === r.uuid);
          if (bill) bill.syncStatus = r.status === "success" ? "synced" : "pending";
        }
        const remaining = bills.filter((b) => b.syncStatus !== "synced");
        await StorageHelper.saveOfflineBills(JSON.stringify(remaining));
        this.offlineCount = remaining.filter((b) => b.syncStatus === "pending").length;
        ToastUtil.showSuccess(`同步完成，共${result.successCount}条成功`);
      } else {
        ToastUtil.showError("同步失败，请检查网络");
      }
    } catch (e) {
      ToastUtil.showError("同步异常");
    }
  }

  async onBackup(): Promise<void> {
    await this.onManualSync();
    ToastUtil.showInfo("正在生成备份文件...");
    try {
      const downloadUrl: string = `${AppState.BASE_URL}/api/bill/export`;
      const fileName: string = `bills_backup_${DateUtil.formatCompactDateTime(new Date())}.xlsx`;
      const savedPath: string = await HttpRequest.downloadToFile(downloadUrl, fileName);
      const displayName: string = savedPath.substring(savedPath.lastIndexOf("/") + 1);
      ToastUtil.showSuccess(`Excel备份已保存：${displayName}`);
    } catch (err) {
      console.error("Excel备份失败，尝试CSV导出:", JSON.stringify(err));
      try {
        const csvPath: string = await CsvExportUtil.exportToCsv();
        const displayName: string = csvPath.substring(csvPath.lastIndexOf("/") + 1);
        ToastUtil.showSuccess(`已导出CSV备份：${displayName}`);
      } catch (csvErr) {
        console.error("CSV备份也失败:", JSON.stringify(csvErr));
        ToastUtil.showError("备份失败：" + (err?.message || "网络异常"));
      }
    }
  }

  async onLogout(): Promise<void> {
    this.logoutDialogController.close();
    await AppState.logout();
    // 退出后 themeMode 被重置为 light，同步 isDark
    this.isDark = false;
    router.replaceUrl({ url: "pages/auth/LoginPage" });
    ToastUtil.showSuccess("已退出登录");
  }

  build() {
    Column() {
      this.buildTopBar()
      Scroll() {
        Column() {
          this.buildSectionGeneral()

          if (this.showPasscodeSetup) {
            this.buildPasscodeForm();
          }

          this.buildSectionData()
          this.buildSectionAbout()
          Button("退出登录")
            .fontSize(14)
            .fontColor($r("app.color.danger"))
            .backgroundColor($r("app.color.bg_card"))
            .border({ width: 1, color: $r("app.color.danger") })
            .borderRadius(20)
            .height(44)
            .width("60%")
            .margin({ top: 30 })
            .onClick(() => {
              this.logoutDialogController.open();
            })
          Text("© 2026 胡宪棋 | 软件2413 | 学号202421332084")
            .fontSize(11)
            .fontColor($r("app.color.text_hint"))
            .margin({ top: 10, bottom: 20 })
        }
        .padding({ left: 16, right: 16, bottom: 30 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width("100%")
    .height("100%")
    .backgroundColor($r("app.color.bg_page"));
  }

  @Builder
  buildTopBar() {
    Row() {
      Image($r("sys.media.ohos_ic_public_arrow_left"))
        .width(24)
        .height(24)
        .onClick(() => {
          router.back();
        })
      Text("设置")
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .margin({ left: 8 })
      Blank()
    }
    .width("100%")
    .padding({ left: 16, right: 16, top: 40, bottom: 10 })
    .backgroundColor($r("app.color.bg_card"));
  }

  @Builder
  buildSectionGeneral() {
    Column() {
      Text("⚙️ 通用设置")
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ bottom: 10 })
      Column() {
        // 深色主题开关：isDark 控制 Toggle 小球位置，themeMode 驱动资源文件切换
        this.buildThemeSwitch()

        this.buildPasscodeSwitch()
      }
      .width("100%")
      .padding(16)
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
    }
    .width("100%")
    .margin({ top: 16 });
  }

  /**
   * 深色主题专用开关行
   * 单独抽出是为了直接绑定 @State isDark，确保 Toggle 小球即时响应
   */
  @Builder
  buildThemeSwitch() {
    Row() {
      Column() {
        Text("深色主题")
          .fontSize(14)
          .fontColor($r("app.color.text_primary"))
        Text("开启深色模式保护眼睛")
          .fontSize(11)
          .fontColor($r("app.color.text_hint"))
          .margin({ top: 2 })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      Toggle({ type: ToggleType.Switch, isOn: this.isDark })
        .onChange((v: boolean) => {
          this.onToggleTheme(v);
        })
    }
    .width("100%")
    .padding({ top: 4, bottom: 4 });
  }

  @Builder
  buildSectionData() {
    Column() {
      Text("💾 数据管理")
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ bottom: 10 })
      Column() {
        this.buildTapItem("手动同步离线账单", `${this.offlineCount}条待同步`, () => {
          this.onManualSync();
        })
        this.buildTapItem("账单文件备份", "导出Excel保存至手机", () => {
          this.onBackup();
        })
      }
      .width("100%")
      .padding(16)
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
    }
    .width("100%")
    .margin({ top: 16 });
  }

  @Builder
  buildSectionAbout() {
    Column() {
      Text("ℹ️ 关于")
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ bottom: 10 })
      Column() {
        this.buildAboutContent()
      }
      .width("100%")
      .padding(16)
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
    }
    .width("100%")
    .margin({ top: 16 });
  }

  @Builder
  buildPasscodeSwitch() {
    Row() {
      Column() {
        Text("应用密码锁")
          .fontSize(14)
          .fontColor($r("app.color.text_primary"))
        Text(this.passcodeEnabled ? "已开启，进入应用需验证" : "关闭")
          .fontSize(11)
          .fontColor($r("app.color.text_hint"))
          .margin({ top: 2 })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      Row() {
        // 自定义滑动开关：避免 Toggle.onChange 在页面生命周期中的竞态条件
        Row() {
          Circle({ width: 20, height: 20 })
            .fill(Color.White)
            .shadow({
              radius: 2,
              color: "#33000000",
              offsetX: 0,
              offsetY: 1,
            })
        }
        .width(24)
        .height(24)
        .borderRadius(12)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(this.passcodeEnabled ? $r("app.color.primary") : "#FFCCCCCC")
        .animation({ duration: 150, curve: Curve.EaseInOut })
        .margin({
          left: this.passcodeEnabled ? 16 : 0,
          right: this.passcodeEnabled ? 0 : 16,
        })
      }
      .width(40)
      .height(24)
      .borderRadius(12)
      .backgroundColor(this.passcodeEnabled ? "#4CAF50" : "#FFDDDDDD")
      .justifyContent(this.passcodeEnabled ? FlexAlign.End : FlexAlign.Start)
      .alignItems(VerticalAlign.Center)
      .animation({ duration: 150, curve: Curve.EaseInOut })
      .onClick(() => {
        console.info("[SettingsPage] 用户点击密码锁开关, 当前状态=" + this.passcodeEnabled);
        this.userToggling = true;
        this.onTogglePasscode(!this.passcodeEnabled);
      })
    }
    .width("100%")
    .padding({ top: 4, bottom: 4 });
  }

  @Builder
  buildTapItem(title: string, desc: string, onTap: () => void) {
    Row() {
      Column() {
        Text(title)
          .fontSize(14)
          .fontColor($r("app.color.text_primary"))
        Text(desc)
          .fontSize(11)
          .fontColor($r("app.color.text_hint"))
          .margin({ top: 2 })
      }
      .layoutWeight(1)
      .alignItems(HorizontalAlign.Start)
      Image($r("sys.media.ohos_ic_public_arrow_right"))
        .width(16)
        .height(16)
        .fillColor($r("app.color.text_hint"))
    }
    .width("100%")
    .padding({ top: 8, bottom: 8 })
    .onClick(onTap);
  }

  @Builder
  buildPasscodeForm() {
    Column() {
      Text("设置数字密码")
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .fontColor($r("app.color.text_primary"))
        .margin({ bottom: 10 })
      TextInput({ placeholder: "4-6位数字密码", text: this.passcodeInput })
        .type(InputType.Password)
        .maxLength(6)
        .fontSize(14)
        .height(40)
        .backgroundColor($r("app.color.bg_page"))
        .borderRadius(8)
        .width("100%")
        .padding({ left: 10, right: 10 })
        .onChange((v: string) => {
          this.passcodeInput = v;
        })
      TextInput({ placeholder: "确认密码", text: this.passcodeConfirm })
        .type(InputType.Password)
        .maxLength(6)
        .fontSize(14)
        .height(40)
        .backgroundColor($r("app.color.bg_page"))
        .borderRadius(8)
        .width("100%")
        .padding({ left: 10, right: 10 })
        .margin({ top: 8 })
        .onChange((v: string) => {
          this.passcodeConfirm = v;
        })
      Row() {
        Button("取消")
          .fontSize(13)
          .backgroundColor($r("app.color.bg_page"))
          .fontColor($r("app.color.text_secondary"))
          .borderRadius(18)
          .height(36)
          .flexGrow(1)
          .onClick(() => {
            this.showPasscodeSetup = false;
            this.passcodeEnabled = false;
            this.passcodeInput = "";
            this.passcodeConfirm = "";
          })
        Button("确认")
          .fontSize(13)
          .backgroundColor($r("app.color.primary"))
          .fontColor(Color.White)
          .borderRadius(18)
          .height(36)
          .flexGrow(1)
          .margin({ left: 10 })
          .onClick(() => {
            this.onSavePasscode();
          })
      }
      .width("100%")
      .margin({ top: 12 })
    }
    .width("100%")
    .padding(16)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(12)
    .margin({ top: 4 });
  }

  @Builder
  buildAboutContent() {
    Column() {
      Text("个人智能理财系统")
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
      Column() {
        this.buildAboutRow("姓名", "胡宪棋")
        this.buildAboutRow("班级", "软件2413")
        this.buildAboutRow("学号", "202421332084")
        this.buildAboutRow("版本", "V1.0 (2026-06-23)")
        this.buildAboutRow("后端", "SpringBoot 3.x + MyBatis-Plus")
        this.buildAboutRow("前端", "HarmonyOS ArkTS + Router")
        this.buildAboutRow("数据", "MySQL 8.0 + Preferences")
      }
      .width("100%")
      .margin({ top: 12 })
      Text("软件功能介绍")
        .fontSize(14)
        .fontWeight(FontWeight.Medium)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ top: 16, bottom: 6 })
      Text(
        "本软件是一款基于鸿蒙生态的智能个人理财管理工具，旨在帮助用户养成理性消费和科学理财的习惯。" +
          "主要功能包括：\n\n" +
          "1. 记账管理：支持快速录入收支账单，包括金额、消费分类、文字备注、小票拍照上传。独特的离线记账能力确保用户即使无网络也能正常记账，WiFi恢复后自动同步至后端数据库。\n\n" +
          "2. 账单管理：提供完整账单流水查阅，支持按月筛选、收支类型筛选、关键词搜索。账单支持编辑修正和删除操作，并可一键导出Excel表格保存至手机。\n\n" +
          "3. 月度统计：自动聚合当月及历史月份财务数据，通过饼图和柱状图直观展示支出分类占比和每日收支趋势，帮助用户全面掌握资金流向。\n\n" +
          "4. 预算管理：支持设置月度总预算和各消费分类子预算，通过可视化进度条实时追踪消费进度。预算超80%时系统自动预警，有效防止超支。\n\n" +
          "5. 存钱目标：支持创建存钱小目标（如买手机、旅行等），追踪存款进度。完成目标后自动归档，激励用户坚持储蓄。\n\n" +
          "6. AI智能分析：调用DeepSeek大模型对当月消费数据进行深度分析，识别冗余消费项、不良消费习惯，并提供个性化省钱方案和月度财务复盘报告。\n\n" +
          "7. 安全与个性化：支持浅色/深色主题一键切换、4-6位数字密码锁保护隐私、JWT令牌保障数据安全。所有用户数据通过后端接口隔离，确保隐私不被泄露。",
      )
        .fontSize(12)
        .fontColor($r("app.color.text_secondary"))
        .lineHeight(20)
        .width("100%")
    };
  }

  @Builder
  buildAboutRow(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(12)
        .fontColor($r("app.color.text_hint"))
        .width(60)
      Text(value)
        .fontSize(13)
        .fontColor($r("app.color.text_primary"))
        .layoutWeight(1)
    }
    .width("100%")
    .padding({ top: 6, bottom: 6 });
  }
}
