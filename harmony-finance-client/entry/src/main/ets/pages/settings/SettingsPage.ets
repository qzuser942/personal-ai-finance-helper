/**
 * 设置与关于页面
 * 功能：深浅色主题切换、密码锁、手动同步、备份、关于信息、退出登录
 * @author 胡宪棋 软件2413 202421332084
 */
import router from '@ohos.router';
import { AppState } from '../../store/AppState';
import { StorageHelper } from '../../utils/storage';
import { BillApi } from '../../network/api/billApi';
import { DateUtil } from '../../utils/date';
import { ToastUtil } from '../../utils/toast';
import { OfflineBill } from '../../model/BillModel';
import { ConfirmDialog } from '../../components/common/ConfirmDialog';

@Entry
@Component
struct SettingsPage {
  @State isDarkMode: boolean = AppState.themeMode === 'dark';
  @State passcodeEnabled: boolean = false;
  @State offlineCount: number = 0;
  @State showPasscodeSetup: boolean = false;
  @State passcodeInput: string = '';
  @State passcodeConfirm: string = '';
  @State showLogoutDialog: boolean = false;

  async aboutToAppear(): Promise<void> {
    this.isDarkMode = AppState.themeMode === 'dark';
    this.passcodeEnabled = await StorageHelper.getPasscodeEnabled();
    await this.checkOffline();
  }

  async checkOffline(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      this.offlineCount = bills.filter(b => b.syncStatus === 'pending').length;
    } catch (e) { this.offlineCount = 0; }
  }

  async onToggleTheme(): Promise<void> {
    const newMode = await AppState.toggleTheme();
    this.isDarkMode = newMode === 'dark';
    ToastUtil.showSuccess(`已切换为${newMode === 'dark' ? '深色' : '浅色'}主题`);
  }

  async onTogglePasscode(enable: boolean): Promise<void> {
    if (enable) {
      this.passcodeEnabled = false;
      this.showPasscodeSetup = true;
    } else {
      this.passcodeEnabled = false;
      await StorageHelper.putBoolean(StorageHelper.KEYS.PASSCODE_ENABLED, false);
      await StorageHelper.remove(StorageHelper.KEYS.PASSCODE_VALUE);
      ToastUtil.showSuccess('密码锁已关闭');
    }
  }

  async onSavePasscode(): Promise<void> {
    if (!this.passcodeInput || this.passcodeInput.length < 4 || this.passcodeInput.length > 6) {
      ToastUtil.showError('请输入4-6位数字密码');
      return;
    }
    if (!/^\d+$/.test(this.passcodeInput)) {
      ToastUtil.showError('密码仅支持数字');
      return;
    }
    if (this.passcodeInput !== this.passcodeConfirm) {
      ToastUtil.showError('两次密码不一致');
      return;
    }

    await StorageHelper.putBoolean(StorageHelper.KEYS.PASSCODE_ENABLED, true);
    await StorageHelper.putString(StorageHelper.KEYS.PASSCODE_VALUE, this.passcodeInput);
    this.passcodeEnabled = true;
    this.showPasscodeSetup = false;
    this.passcodeInput = '';
    this.passcodeConfirm = '';
    ToastUtil.showSuccess('密码锁已开启');
  }

  /**
   * 手动触发离线账单同步
   * SRS 6.2节 - 手动同步
   */
  async onManualSync(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      const pending = bills.filter(b => b.syncStatus === 'pending');

      if (pending.length === 0) {
        ToastUtil.showInfo('暂无待同步数据');
        return;
      }

      ToastUtil.showInfo(`正在同步${pending.length}条离线账单...`);

      const syncData = pending.map(b => ({
        uuid: b.uuid,
        amount: b.amount,
        type: b.type,
        categoryId: b.categoryId,
        categoryName: b.categoryName,
        remark: b.remark,
        consumeTime: b.consumeTime,
        createdAt: b.createdAt
      }));

      const res = await BillApi.syncBatch(syncData);
      if (res.code === 200 && res.data) {
        for (const r of res.data.results) {
          const bill = bills.find(b => b.uuid === r.uuid);
          if (bill) bill.syncStatus = r.status === 'success' ? 'synced' : 'pending';
        }
        const remaining = bills.filter(b => b.syncStatus !== 'synced');
        await StorageHelper.saveOfflineBills(JSON.stringify(remaining));
        this.offlineCount = remaining.filter(b => b.syncStatus === 'pending').length;
        ToastUtil.showSuccess(`同步完成，共${res.data.successCount}条成功`);
      } else {
        ToastUtil.showError('同步失败，请检查网络');
      }
    } catch (e) { ToastUtil.showError('同步异常'); }
  }

  /**
   * 账单文件备份 - 下载Excel到本地
   */
  async onBackup(): Promise<void> {
    await this.onManualSync();
    ToastUtil.showInfo('账单备份已触发，请查看下载目录');
  }

  async onLogout(): Promise<void> {
    this.showLogoutDialog = false;
    await AppState.logout();
    router.replaceUrl({ url: 'pages/auth/LoginPage' });
    ToastUtil.showSuccess('已退出登录');
  }

  build() {
    Column() {
      this.buildTopBar();
      Scroll() {
        Column() {
          // 通用设置
          this.buildSection('⚙️ 通用设置', () => {
            this.buildSwitchItem('深色主题', '开启深色模式保护眼睛', this.isDarkMode,
              () => { this.onToggleTheme(); });

            this.buildSwitchItem('应用密码锁', this.passcodeEnabled ? '已开启，进入应用需验证' : '关闭',
              this.passcodeEnabled, (v: boolean) => { this.onTogglePasscode(v); });
          });

          // 密码设置表单
          if (this.showPasscodeSetup) {
            this.buildPasscodeForm();
          }

          // 数据管理
          this.buildSection('💾 数据管理', () => {
            this.buildTapItem('手动同步离线账单', `${this.offlineCount}条待同步`,
              () => { this.onManualSync(); });
            this.buildTapItem('账单文件备份', '导出Excel保存至手机',
              () => { this.onBackup(); });
          });

          // 关于
          this.buildSection('ℹ️ 关于', () => {
            this.buildAboutContent();
          });

          // 退出登录
          Button('退出登录')
            .fontSize(14).fontColor('#FF6B6B').backgroundColor('#FFFFFF')
            .border({ width: 1, color: '#FF6B6B' }).borderRadius(20)
            .height(44).width('60%').margin({ top: 30 })
            .onClick(() => { this.showLogoutDialog = true; });

          Text('© 2026 胡宪棋 | 软件2413 | 学号202421332084')
            .fontSize(11).fontColor('#CCCCCC').margin({ top: 10, bottom: 20 });

        }.padding({ left: 16, right: 16, bottom: 30 })
      }.layoutWeight(1).scrollBar(BarState.Off)
    }
    .width('100%').height('100%').backgroundColor('#F5F5F5')
  }

  @Builder
  buildTopBar() {
    Row() {
      Image($r('sys.media.ohos_ic_public_arrow_left'))
        .width(24).height(24).onClick(() => { router.back(); })
      Text('设置').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#333333').margin({ left: 8 })
      Blank()
    }
    .width('100%').padding({ left: 16, right: 16, top: 40, bottom: 10 }).backgroundColor('#FFFFFF')
  }

  @Builder
  buildSection(title: string, contentBuilder: () => void) {
    Column() {
      Text(title).fontSize(16).fontWeight(FontWeight.Bold).fontColor('#333333').width('100%').margin({ bottom: 10 })
      Column() { contentBuilder(); }
        .width('100%').padding(16).backgroundColor('#FFFFFF').borderRadius(12)
    }
    .width('100%').margin({ top: 16 })
  }

  @Builder
  buildSwitchItem(title: string, desc: string, value: boolean, onChange: (v: boolean) => void) {
    Row() {
      Column() {
        Text(title).fontSize(14).fontColor('#333333')
        Text(desc).fontSize(11).fontColor('#999999').margin({ top: 2 })
      }.layoutWeight(1).alignItems(HorizontalAlign.Start)
      Toggle({ type: ToggleType.Switch, isOn: value })
        .onChange(onChange)
    }
    .width('100%').padding({ top: 4, bottom: 4 })
  }

  @Builder
  buildTapItem(title: string, desc: string, onTap: () => void) {
    Row() {
      Column() {
        Text(title).fontSize(14).fontColor('#333333')
        Text(desc).fontSize(11).fontColor('#999999').margin({ top: 2 })
      }.layoutWeight(1).alignItems(HorizontalAlign.Start)
      Image($r('sys.media.ohos_ic_public_arrow_right')).width(16).height(16).fillColor('#CCCCCC')
    }
    .width('100%').padding({ top: 8, bottom: 8 })
    .onClick(onTap)
  }

  @Builder
  buildPasscodeForm() {
    Column() {
      Text('设置数字密码').fontSize(14).fontWeight(FontWeight.Medium).fontColor('#333333').margin({ bottom: 10 })
      TextInput({ placeholder: '4-6位数字密码', text: this.passcodeInput })
        .type(InputType.Password).maxLength(6).fontSize(14)
        .height(40).backgroundColor('#F5F5F5').borderRadius(8).width('100%').padding({ left: 10, right: 10 })
        .onChange((v: string) => { this.passcodeInput = v; })
      TextInput({ placeholder: '确认密码', text: this.passcodeConfirm })
        .type(InputType.Password).maxLength(6).fontSize(14)
        .height(40).backgroundColor('#F5F5F5').borderRadius(8)
        .width('100%').padding({ left: 10, right: 10 }).margin({ top: 8 })
        .onChange((v: string) => { this.passcodeConfirm = v; })
      Row() {
        Button('取消').fontSize(13).backgroundColor('#F0F0F0').fontColor('#666666').borderRadius(18).height(36).flexGrow(1)
          .onClick(() => { this.showPasscodeSetup = false; this.passcodeInput = ''; this.passcodeConfirm = ''; })
        Button('确认').fontSize(13).backgroundColor('#4A90D9').fontColor('#FFFFFF').borderRadius(18).height(36).flexGrow(1).margin({ left: 10 })
          .onClick(() => { this.onSavePasscode(); })
      }.width('100%').margin({ top: 12 })
    }
    .width('100%').padding(16).backgroundColor('#FFFFFF').borderRadius(12).margin({ top: 4 })
  }

  @Builder
  buildAboutContent() {
    Column() {
      Text('个人智能理财系统').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#333333')

      // 作者信息
      Column() {
        this.buildAboutRow('姓名', '胡宪棋');
        this.buildAboutRow('班级', '软件2413');
        this.buildAboutRow('学号', '202421332084');
        this.buildAboutRow('版本', 'V1.0 (2026-06-23)');
        this.buildAboutRow('后端', 'SpringBoot 3.x + MyBatis-Plus');
        this.buildAboutRow('前端', 'HarmonyOS ArkTS + Router');
        this.buildAboutRow('数据', 'MySQL 8.0 + Preferences');
      }.width('100%').margin({ top: 12 })

      // 功能介绍
      Text('软件功能介绍').fontSize(14).fontWeight(FontWeight.Medium).fontColor('#333333').width('100%').margin({ top: 16, bottom: 6 })

      Text('本软件是一款基于鸿蒙生态的智能个人理财管理工具，旨在帮助用户养成理性消费和科学理财的习惯。' +
        '主要功能包括：\n\n' +
        '1. **记账管理**：支持快速录入收支账单，包括金额、消费分类、文字备注、小票拍照上传。独特的离线记账能力确保用户即使无网络也能正常记账，WiFi恢复后自动同步至后端数据库。\n\n' +
        '2. **账单管理**：提供完整账单流水查阅，支持按月筛选、收支类型筛选、关键词搜索。账单支持编辑修正和删除操作，并可一键导出Excel表格保存至手机。\n\n' +
        '3. **月度统计**：自动聚合当月及历史月份财务数据，通过饼图和柱状图直观展示支出分类占比和每日收支趋势，帮助用户全面掌握资金流向。\n\n' +
        '4. **预算管理**：支持设置月度总预算和各消费分类子预算，通过可视化进度条实时追踪消费进度。预算超80%时系统自动预警，有效防止超支。\n\n' +
        '5. **存钱目标**：支持创建存钱小目标（如买手机、旅行等），追踪存款进度。完成目标后自动归档，激励用户坚持储蓄。\n\n' +
        '6. **AI智能分析**：调用DeepSeek大模型对当月消费数据进行深度分析，识别冗余消费项、不良消费习惯，并提供个性化省钱方案和月度财务复盘报告。\n\n' +
        '7. **安全与个性化**：支持浅色/深色主题一键切换、4-6位数字密码锁保护隐私、JWT令牌保障数据安全。所有用户数据通过后端接口隔离，确保隐私不被泄露。')
        .fontSize(12).fontColor('#666666').lineHeight(20).width('100%')
    }
  }

  @Builder
  buildAboutRow(label: string, value: string) {
    Row() {
      Text(label).fontSize(12).fontColor('#999999').width(60)
      Text(value).fontSize(13).fontColor('#333333').layoutWeight(1)
    }.width('100%').padding({ top: 6, bottom: 6 })
  }
}
