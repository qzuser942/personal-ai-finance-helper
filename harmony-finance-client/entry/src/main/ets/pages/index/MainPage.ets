/**
 * 主页 - 底部Tab导航容器
 * 包含：记账(内联)、账单、统计、目标、AI、设置
 * @author 胡宪棋 软件2413 202421332084
 */
import router from '@ohos.router';
import { CategoryApi } from '../../network/api/categoryApi';
import { BillApi } from '../../network/api/billApi';
import { StorageHelper, KEYS } from '../../utils/storage';
import { DateUtil } from '../../utils/date';
import { ToastUtil } from '../../utils/toast';
import { Category } from '../../model/CategoryModel';

@Entry
@Component
struct MainPage {
  @State currentTab: number = 0;

  private readonly TAB_TITLES: string[] = ['记账', '账单', '统计', '目标', 'AI', '设置'];
  private readonly TAB_ICONS: string[] = ['💰', '📋', '📊', '🎯', '🤖', '⚙️'];

  build() {
    Tabs({ barPosition: BarPosition.End, index: this.currentTab }) {
      TabContent() {
        RecordPage()
      }.tabBar(this.tabBuilder(0))

      TabContent() {
        PlaceHolderView({ message: '账单流水', icon: '📋' })
      }.tabBar(this.tabBuilder(1))

      TabContent() {
        PlaceHolderView({ message: '月度统计', icon: '📊' })
      }.tabBar(this.tabBuilder(2))

      TabContent() {
        PlaceHolderView({ message: '预算与存钱目标', icon: '🎯' })
      }.tabBar(this.tabBuilder(3))

      TabContent() {
        PlaceHolderView({ message: 'AI理财分析', icon: '🤖' })
      }.tabBar(this.tabBuilder(4))

      TabContent() {
        PlaceHolderView({ message: '设置', icon: '⚙️' })
      }.tabBar(this.tabBuilder(5))
    }
    .barHeight(56)
    .barBackgroundColor('#FFFFFF')
    .animationDuration(0)
    .scrollable(false)
    .onChange((index: number) => {
      this.currentTab = index;
      this.navigateToTab(index);
    })
    .width('100%')
    .height('100%')
  }

  @Builder
  tabBuilder(index: number) {
    Column() {
      Text(this.TAB_ICONS[index])
        .fontSize(20)
        .margin({ bottom: 1 })
      Text(this.TAB_TITLES[index])
        .fontSize(10)
        .fontColor(this.currentTab === index ? '#4A90D9' : '#999999')
    }
    .width('100%')
    .padding({ top: 4, bottom: 4 })
    .alignItems(HorizontalAlign.Center)
  }

  navigateToTab(index: number): void {
    const routes: Record<number, string> = {
      1: 'pages/bill/BillListPage',
      2: 'pages/statistics/StatisticsPage',
      3: 'pages/budget/BudgetPage',
      4: 'pages/ai/AiAnalysisPage',
      5: 'pages/settings/SettingsPage'
    };

    if (routes[index]) {
      router.pushUrl({ url: routes[index] });
      // Reset tab back to recording
      this.currentTab = 0;
    }
  }
}

/**
 * 占位视图 - 用于非记账Tab
 */
@Component
struct PlaceHolderView {
  @Prop message: string = '';
  @Prop icon: string = '';

  build() {
    Column() {
      Text(this.icon).fontSize(48).opacity(0.3)
      Text(this.message).fontSize(14).fontColor('#999999').margin({ top: 8 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#F5F5F5')
  }
}

/**
 * 记账页核心组件
 */
@Component
struct RecordPage {
  @State billType: string = 'expense';
  @State amount: string = '';
  @State selectedCategory: Category | null = null;
  @State remark: string = '';
  @State categories: Category[] = [];
  @State showCategoryPanel: boolean = false;
  @State offlineCount: number = 0;
  @State isSubmitting: boolean = false;
  private readonly quickAmounts: number[] = [10, 20, 50, 100, 200];

  async aboutToAppear(): Promise<void> {
    await this.loadCategories();
    await this.checkOffline();
    // 尝试自动同步离线数据
    await this.autoSync();
  }

  async loadCategories(): Promise<void> {
    try {
      const res = await CategoryApi.getList(this.billType);
      if (res.code === 200 && res.data) {
        this.categories = res.data;
      }
    } catch (e) {
      console.error('加载分类失败:', JSON.stringify(e));
    }
  }

  async checkOffline(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: object[] = JSON.parse(json);
      this.offlineCount = bills.filter((b: Record<string, string>) =>
        b['syncStatus'] === 'pending').length;
    } catch (e) {
      this.offlineCount = 0;
    }
  }

  async autoSync(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: Record<string, object>[] = JSON.parse(json);
      const pending = bills.filter((b: Record<string, string>) =>
        b['syncStatus'] === 'pending');

      if (pending.length === 0) return;

      const syncData = pending.map((b: Record<string, object>) => ({
        uuid: b['uuid'],
        amount: b['amount'],
        type: b['type'],
        categoryId: b['categoryId'],
        categoryName: b['categoryName'],
        remark: b['remark'],
        consumeTime: b['consumeTime'],
        createdAt: b['createdAt']
      }));

      const res = await BillApi.syncBatch(syncData);
      if (res.code === 200 && res.data) {
        for (const r of res.data.results) {
          const bill = bills.find((b: Record<string, string>) =>
            b['uuid'] === r.uuid);
          if (bill) {
            bill['syncStatus'] = r.status === 'success' ? 'synced' : 'pending';
          }
        }
        const remaining = bills.filter((b: Record<string, string>) =>
          b['syncStatus'] !== 'synced');
        await StorageHelper.saveOfflineBills(JSON.stringify(remaining));
        this.offlineCount = remaining.filter((b: Record<string, string>) =>
          b['syncStatus'] === 'pending').length;

        if (res.data.successCount > 0) {
          ToastUtil.showSuccess(`已自动同步${res.data.successCount}条离线账单`);
        }
      }
    } catch (e) { /* ignore */ }
  }

  async onSubmit(): Promise<void> {
    const amt = parseFloat(this.amount);
    if (!amt || amt <= 0) {
      ToastUtil.showError('请输入有效金额');
      return;
    }
    if (!this.selectedCategory) {
      ToastUtil.showError('请选择消费分类');
      return;
    }

    this.isSubmitting = true;
    const billData = {
      amount: amt,
      type: this.billType,
      categoryId: this.selectedCategory.id,
      remark: this.remark,
      consumeTime: DateUtil.getCurrentDateTimeStr()
    };

    try {
      const res = await BillApi.add(billData);
      if (res.code === 200) {
        ToastUtil.showSuccess('记账成功');
        this.clearForm();
        await this.checkOffline();
      } else if (res.code === -1) {
        await this.saveOffline(billData);
      } else {
        ToastUtil.showError(res.message || '记账失败');
      }
    } catch (e) {
      await this.saveOffline(billData);
    }
    this.isSubmitting = false;
  }

  async saveOffline(billData: Record<string, object>): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    const bills: Record<string, object>[] = JSON.parse(json);
    const pendingCount = bills.filter((b: Record<string, string>) =>
      b['syncStatus'] === 'pending').length;

    if (pendingCount >= 100) {
      ToastUtil.showWarning('离线队列已达上限（100条），请联网同步后再记账');
      return;
    }

    const offline: Record<string, object> = {
      uuid: DateUtil.generateUUID(),
      amount: billData['amount'],
      type: billData['type'],
      categoryId: billData['categoryId'],
      categoryName: this.selectedCategory?.name || '',
      remark: billData['remark'] || '',
      consumeTime: billData['consumeTime'],
      createdAt: DateUtil.getCurrentDateTimeStr(),
      offlineCreatedAt: DateUtil.getCurrentDateTimeStr(),
      syncStatus: 'pending',
      hasImage: false
    };

    bills.push(offline);
    await StorageHelper.saveOfflineBills(JSON.stringify(bills));
    this.offlineCount = bills.filter((b: Record<string, string>) =>
      b['syncStatus'] === 'pending').length;
    ToastUtil.showSuccess('已离线保存，联网后自动同步');
    this.clearForm();
  }

  clearForm(): void {
    this.amount = '';
    this.remark = '';
    this.selectedCategory = null;
  }

  onTypeChange(type: string): void {
    this.billType = type;
    this.selectedCategory = null;
    this.loadCategories();
  }

  build() {
    Column() {
      // 顶部栏
      Row() {
        Text('💰 智能理财')
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')

        Blank()

        if (this.offlineCount > 0) {
          Row() {
            Text(`⏳ ${this.offlineCount}待同步`)
              .fontSize(11)
              .fontColor('#FF9800')
          }
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
          .backgroundColor('#FFF3E0')
          .borderRadius(12)
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 40, bottom: 10 })
      .backgroundColor('#FFFFFF')

      Scroll() {
        Column() {
          // 收支类型切换
          Row() {
            Button('📤 支出')
              .fontSize(14)
              .backgroundColor(this.billType === 'expense' ? '#FF6B6B' : '#F0F0F0')
              .fontColor(this.billType === 'expense' ? '#FFFFFF' : '#999999')
              .borderRadius(20)
              .height(36)
              .flexGrow(1)
              .onClick(() => { this.onTypeChange('expense'); })

            Button('📥 收入')
              .fontSize(14)
              .backgroundColor(this.billType === 'income' ? '#4CAF50' : '#F0F0F0')
              .fontColor(this.billType === 'income' ? '#FFFFFF' : '#999999')
              .borderRadius(20)
              .height(36)
              .flexGrow(1)
              .margin({ left: 10 })
              .onClick(() => { this.onTypeChange('income'); })
          }
          .width('100%')
          .margin({ top: 14 })

          // 金额输入
          Column() {
            Text(this.billType === 'expense' ? '消费金额' : '收入金额')
              .fontSize(13)
              .fontColor('#999999')
              .margin({ bottom: 8 })
            Row() {
              Text('¥')
                .fontSize(28)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')
              TextInput({ placeholder: '0.00', text: this.amount })
                .fontSize(32)
                .fontWeight(FontWeight.Bold)
                .fontColor('#333333')
                .type(InputType.Number)
                .backgroundColor(Color.Transparent)
                .layoutWeight(1)
                .onChange((v: string) => { this.amount = v; })
            }
            .width('100%')
            .height(65)
            .padding({ left: 12, right: 12 })
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
          }
          .width('100%')
          .margin({ top: 16 })

          // 快捷金额
          Row() {
            ForEach(this.quickAmounts, (amt: number) => {
              Button(`¥${amt}`)
                .fontSize(13)
                .backgroundColor(this.amount === amt.toString() ? '#4A90D9' : '#F0F0F0')
                .fontColor(this.amount === amt.toString() ? '#FFFFFF' : '#666666')
                .borderRadius(16)
                .height(30)
                .padding({ left: 12, right: 12 })
                .margin({ right: 8 })
                .onClick(() => { this.amount = amt.toString(); })
            })
          }
          .width('100%')
          .margin({ top: 10 })

          // 分类选择
          Column() {
            Text('消费分类')
              .fontSize(13)
              .fontColor('#999999')
              .margin({ bottom: 8 })
              .width('100%')

            Row() {
              Text(this.selectedCategory ? this.selectedCategory.name : '请选择分类')
                .fontSize(14)
                .fontColor(this.selectedCategory ? '#333333' : '#BBBBBB')
                .layoutWeight(1)
              Image($r('sys.media.ohos_ic_public_arrow_down'))
                .width(16)
                .height(16)
            }
            .width('100%')
            .height(46)
            .padding({ left: 12, right: 12 })
            .backgroundColor('#FFFFFF')
            .borderRadius(12)
            .onClick(() => { this.showCategoryPanel = !this.showCategoryPanel; })

            if (this.showCategoryPanel) {
              Grid() {
                ForEach(this.categories, (cat: Category) => {
                  GridItem() {
                    Column() {
                      Text(this.getCategoryIcon(cat.name))
                        .fontSize(24)
                      Text(cat.name)
                        .fontSize(11)
                        .fontColor('#666666')
                        .margin({ top: 2 })
                    }
                    .width('100%')
                    .height(65)
                    .justifyContent(FlexAlign.Center)
                    .borderRadius(8)
                    .backgroundColor(this.selectedCategory?.id === cat.id ? '#E3F2FD' : '#F5F5F5')
                  }
                  .onClick(() => {
                    this.selectedCategory = cat;
                    this.showCategoryPanel = false;
                  })
                })
              }
              .columnsTemplate('1fr 1fr 1fr 1fr')
              .rowsGap(6)
              .columnsGap(6)
              .width('100%')
              .margin({ top: 6 })
            }
          }
          .width('100%')
          .margin({ top: 16 })

          // 备注输入
          Column() {
            Row() {
              Text('备注')
                .fontSize(13)
                .fontColor('#999999')
              Blank()
              Text(`${this.remark.length}/200`)
                .fontSize(11)
                .fontColor('#CCCCCC')
            }
            .width('100%')
            .margin({ bottom: 6 })

            TextInput({ placeholder: '添加备注说明（选填）', text: this.remark })
              .fontSize(14)
              .height(42)
              .backgroundColor('#FFFFFF')
              .borderRadius(12)
              .width('100%')
              .padding({ left: 12, right: 12 })
              .maxLength(200)
              .onChange((v: string) => { this.remark = v; })
          }
          .width('100%')
          .margin({ top: 16 })

          // 提交按钮
          Button(this.isSubmitting ? '提交中...' : '✓  记一笔')
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .backgroundColor(this.billType === 'expense' ? '#FF6B6B' : '#4CAF50')
            .fontColor('#FFFFFF')
            .borderRadius(24)
            .height(48)
            .width('100%')
            .enabled(!this.isSubmitting)
            .margin({ top: 28 })
            .onClick(() => { this.onSubmit(); })

          Row() {
            Button('保存草稿')
              .fontSize(13)
              .backgroundColor('#F0F0F0')
              .fontColor('#666666')
              .borderRadius(20)
              .height(38)
              .flexGrow(1)
              .onClick(() => { this.onSaveDraft(); })

            Button('取消')
              .fontSize(13)
              .backgroundColor('#F0F0F0')
              .fontColor('#666666')
              .borderRadius(20)
              .height(38)
              .flexGrow(1)
              .margin({ left: 10 })
              .onClick(() => { this.clearForm(); })
          }
          .width('100%')
          .margin({ top: 10 })
        }
        .padding({ left: 16, right: 16, bottom: 100 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  async onSaveDraft(): Promise<void> {
    const draft = {
      amount: this.amount,
      type: this.billType,
      categoryId: this.selectedCategory?.id || 0,
      categoryName: this.selectedCategory?.name || '',
      remark: this.remark,
      consumeTime: DateUtil.getCurrentDateTimeStr()
    };
    await StorageHelper.saveDraft(JSON.stringify(draft));
    ToastUtil.showSuccess('草稿已保存');
  }

  private getCategoryIcon(name: string): string {
    const map: Record<string, string> = {
      '餐饮': '🍽️', '交通': '🚗', '购物': '🛒', '娱乐': '🎮',
      '住房': '🏠', '医疗': '🏥', '教育': '📚', '通讯': '📱',
      '服饰': '👔', '日用品': '🧴', '丽人': '💄', '运动': '⚽',
      '旅行': '✈️', '宠物': '🐱', '数码': '💻', '其他支出': '📌',
      '工资': '💰', '奖金': '🏆', '投资收益': '📈', '兼职': '💼',
      '红包': '🧧', '退款': '↩️', '其他收入': '📌'
    };
    return map[name] || '📋';
  }
}
