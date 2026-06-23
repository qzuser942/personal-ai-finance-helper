/**
 * 首页记账核心页
 * 功能：收支类型切换、金额输入、分类选择、备注、拍照上传、草稿保存、离线记账
 * @author 胡宪棋 软件2413 202421332084
 */
import router from '@ohos.router';
import { CategoryApi } from '../../network/api/categoryApi';
import { BillApi } from '../../network/api/billApi';
import { FileApi } from '../../network/api/fileApi';
import { AiApi } from '../../network/api/aiApi';
import { AppState } from '../../store/AppState';
import { StorageHelper } from '../../utils/storage';
import { DateUtil } from '../../utils/date';
import { ToastUtil } from '../../utils/toast';
import { Category } from '../../model/CategoryModel';
import { DraftBill, OfflineBill } from '../../model/BillModel';
import { AiClassifyResult } from '../../model/AiModel';
import { EmptyView } from '../../components/common/EmptyView';
import camera from '@ohos.multimedia.camera';
import picker from '@ohos.multimedia.mediaLibrary';

@Entry
@Component
struct IndexPage {
  // ==================== 状态变量 ====================
  @State billType: string = 'expense';       // income | expense
  @State amount: string = '';
  @State selectedCategory: Category | null = null;
  @State remark: string = '';
  @State categories: Category[] = [];
  @State isLoading: boolean = false;
  @State isSubmitting: boolean = false;
  @State isOnline: boolean = true;
  @State offlineCount: number = 0;
  @State receiptImagePath: string = '';
  @State aiClassifyResult: AiClassifyResult | null = null;
  @State isAiClassifying: boolean = false;
  @State showCategoryPanel: boolean = false;

  // 快捷金额
  private readonly quickAmounts: number[] = [10, 20, 50, 100, 200];

  async aboutToAppear(): Promise<void> {
    // 加载分类
    await this.loadCategories();
    // 加载草稿
    await this.loadDraft();
    // 检查离线队列
    await this.checkOfflineBills();
    // 自动同步离线账单
    await this.autoSync();
  }

  async loadCategories(): Promise<void> {
    try {
      const res = await CategoryApi.getList(this.billType);
      if (res.code === 200 && res.data) {
        this.categories = res.data;
      }
    } catch (err) {
      console.error('加载分类失败:', JSON.stringify(err));
    }
  }

  async loadDraft(): Promise<void> {
    const draftJson = await StorageHelper.getDraft();
    if (draftJson) {
      try {
        const draft: DraftBill = JSON.parse(draftJson);
        this.billType = draft.type || 'expense';
        this.amount = draft.amount || '';
        this.remark = draft.remark || '';
        await this.loadCategories();
        if (draft.categoryId) {
          const cat = this.categories.find(c => c.id === draft.categoryId);
          if (cat) this.selectedCategory = cat;
        }
      } catch (e) { /* ignore */ }
    }
  }

  async checkOfflineBills(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      this.offlineCount = bills.filter(b => b.syncStatus === 'pending').length;
    } catch (e) {
      this.offlineCount = 0;
    }
  }

  /**
   * 联网自动同步离线账单
   * SRS 6.2节：APP冷/热启动时自动检测并同步
   */
  async autoSync(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      const pending = bills.filter(b => b.syncStatus === 'pending');
      if (pending.length === 0) return;

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
        // 更新本地状态
        const results = res.data.results;
        for (const r of results) {
          const bill = bills.find(b => b.uuid === r.uuid);
          if (bill) {
            bill.syncStatus = r.status === 'success' ? 'synced' : 'pending';
          }
        }
        // 清除已同步的记录
        const remaining = bills.filter(b => b.syncStatus !== 'synced');
        await StorageHelper.saveOfflineBills(JSON.stringify(remaining));
        this.offlineCount = remaining.filter(b => b.syncStatus === 'pending').length;

        if (res.data.successCount > 0) {
          ToastUtil.showSuccess(`已自动同步${res.data.successCount}条离线账单`);
        }
      }
    } catch (e) { /* ignore */ }
  }

  async onTypeChange(type: string): Promise<void> {
    this.billType = type;
    this.selectedCategory = null;
    await this.loadCategories();
  }

  onQuickAmount(amt: number): void {
    this.amount = amt.toString();
  }

  onCategorySelect(cat: Category): void {
    this.selectedCategory = cat;
    this.showCategoryPanel = false;
  }

  /**
   * AI智能分类推荐
   */
  async onAiClassify(): Promise<void> {
    if (!this.remark.trim()) {
      ToastUtil.showInfo('请先输入消费备注');
      return;
    }
    this.isAiClassifying = true;
    try {
      const res = await AiApi.classify({
        remark: this.remark,
        type: this.billType
      });
      if (res.code === 200 && res.data) {
        this.aiClassifyResult = res.data;
        const cat = this.categories.find(c => c.id === res.data!.categoryId);
        if (cat) {
          this.selectedCategory = cat;
          ToastUtil.showSuccess(`AI推荐：${cat.name}（置信度${(res.data.confidence * 100).toFixed(0)}%）`);
        }
      }
    } catch (err) { /* ignore */ } finally {
      this.isAiClassifying = false;
    }
  }

  /**
   * 选择图片（拍照或从相册）
   */
  async onPickImage(): Promise<void> {
    try {
      const photoSelectOptions = new picker.PhotoSelectOptions();
      photoSelectOptions.MIMEType = picker.PhotoViewMIMETypes.IMAGE_TYPE;
      photoSelectOptions.maxSelectNumber = 1;

      const photoViewPicker = new picker.PhotoViewPicker();
      const result = await photoViewPicker.select(photoSelectOptions);
      if (result && result.photoUris && result.photoUris.length > 0) {
        this.receiptImagePath = result.photoUris[0];
        ToastUtil.showSuccess('图片已选择');
      }
    } catch (err) {
      ToastUtil.showError('选择图片失败');
    }
  }

  /**
   * 核心提交逻辑 - 区分联网/离线
   * SRS 6.1节：离线缓存机制
   */
  async onSubmit(): Promise<void> {
    // 校验
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

    try {
      // 上传图片（如有）
      let receiptImage = '';
      if (this.receiptImagePath) {
        const uploadRes = await FileApi.uploadReceipt(this.receiptImagePath, 'receipt.jpg');
        if (uploadRes.code === 200 && uploadRes.data) {
          receiptImage = uploadRes.data.filePath;
        }
      }

      const billData = {
        amount: amt,
        type: this.billType,
        categoryId: this.selectedCategory.id,
        remark: this.remark,
        receiptImage: receiptImage,
        consumeTime: DateUtil.getCurrentDateTimeStr()
      };

      const res = await BillApi.add(billData);

      if (res.code === 200) {
        // 联网成功 - 清除草稿
        await StorageHelper.clearDraft();
        ToastUtil.showSuccess('记账成功');
        this.clearForm();
      } else if (res.code === -1) {
        // 网络异常 - 存入离线队列
        await this.saveToOfflineQueue(billData);
      } else {
        ToastUtil.showError(res.message || '记账失败');
      }
    } catch (err) {
      // 异常情况 - 存入离线队列
      const amt = parseFloat(this.amount);
      await this.saveToOfflineQueue({
        amount: amt,
        type: this.billType,
        categoryId: this.selectedCategory!.id,
        remark: this.remark,
        receiptImage: '',
        consumeTime: DateUtil.getCurrentDateTimeStr()
      });
    } finally {
      this.isSubmitting = false;
    }
  }

  /**
   * 离线缓存 - 存入Preferences离线队列
   * 最多支持100条待同步数据
   */
  async saveToOfflineQueue(billData: object): Promise<void> {
    try {
      const json = await StorageHelper.getOfflineBills();
      const bills: OfflineBill[] = JSON.parse(json);
      const pendingCount = bills.filter(b => b.syncStatus === 'pending').length;

      if (pendingCount >= 100) {
        ToastUtil.showWarning('离线队列已达上限（100条），请联网同步后再记账');
        return;
      }

      const offlineBill: OfflineBill = {
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

      bills.push(offlineBill);
      await StorageHelper.saveOfflineBills(JSON.stringify(bills));
      this.offlineCount = bills.filter(b => b.syncStatus === 'pending').length;

      ToastUtil.showSuccess('已离线保存，联网后自动同步');
      this.clearForm();
    } catch (e) {
      ToastUtil.showError('离线保存失败');
    }
  }

  /**
   * 保存草稿
   */
  async onSaveDraft(): Promise<void> {
    const draft: DraftBill = {
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

  /**
   * 取消录入 - 清空表单
   */
  onCancel(): void {
    this.clearForm();
  }

  clearForm(): void {
    this.amount = '';
    this.remark = '';
    this.selectedCategory = null;
    this.receiptImagePath = '';
    this.aiClassifyResult = null;
    StorageHelper.clearDraft();
  }

  build() {
    Column() {
      // 顶部导航
      this.buildTopBar();

      // 滚动内容区
      Scroll() {
        Column() {
          // 收支类型切换
          this.buildTypeSwitch();

          // 金额输入区
          this.buildAmountInput();

          // 快捷金额
          this.buildQuickAmounts();

          // 分类选择
          this.buildCategorySection();

          // 备注输入
          this.buildRemarkSection();

          // 图片选择
          this.buildImageSection();

          // 提交按钮
          this.buildSubmitButtons();
        }
        .padding({ left: 16, right: 16, bottom: 80 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }

  // ==================== 子组件构建方法 ====================

  @Builder
  buildTopBar() {
    Row() {
      Text('💰 记账')
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .fontColor('#333333')

      Blank()

      if (this.offlineCount > 0) {
        Row() {
          Text(`⏳ ${this.offlineCount}待同步`)
            .fontSize(11)
            .fontColor('#FF9800')
          Image($r('sys.media.ohos_ic_public_arrow_right'))
            .width(14)
            .height(14)
            .fillColor('#FF9800')
        }
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })
        .backgroundColor('#FFF3E0')
        .borderRadius(12)
        .onClick(() => {
          router.pushUrl({ url: 'pages/settings/SettingsPage' });
        })
      }

      Row() {
        Image($r('sys.media.ohos_ic_public_clock'))
          .width(22)
          .height(22)
          .fillColor('#666666')
      }
      .margin({ left: 8 })
      .onClick(() => {
        router.pushUrl({ url: 'pages/bill/BillListPage' });
      })
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 40, bottom: 14 })
    .backgroundColor('#FFFFFF')
  }

  @Builder
  buildTypeSwitch() {
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
    .margin({ top: 16 })
  }

  @Builder
  buildAmountInput() {
    Column() {
      Text(this.billType === 'expense' ? '消费金额' : '收入金额')
        .fontSize(13)
        .fontColor('#999999')
        .margin({ bottom: 10 })

      Row() {
        Text('¥')
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')
        TextInput({ placeholder: '0.00', text: this.amount })
          .fontSize(36)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')
          .type(InputType.Number)
          .backgroundColor(Color.Transparent)
          .placeholderColor('#DDDDDD')
          .layoutWeight(1)
          .onChange((value: string) => { this.amount = value; })
      }
      .width('100%')
      .padding({ left: 12, right: 12 })
      .height(70)
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
    }
    .width('100%')
    .margin({ top: 20 })
  }

  @Builder
  buildQuickAmounts() {
    Row() {
      ForEach(this.quickAmounts, (amt: number) => {
        Button(`¥${amt}`)
          .fontSize(13)
          .backgroundColor(this.amount === amt.toString() ? '#4A90D9' : '#F0F0F0')
          .fontColor(this.amount === amt.toString() ? '#FFFFFF' : '#666666')
          .borderRadius(16)
          .height(32)
          .padding({ left: 12, right: 12 })
          .margin({ right: 8 })
          .onClick(() => { this.onQuickAmount(amt); })
      })
    }
    .width('100%')
    .margin({ top: 12 })
  }

  @Builder
  buildCategorySection() {
    Column() {
      Text('消费分类')
        .fontSize(13)
        .fontColor('#999999')
        .margin({ bottom: 10 })
        .width('100%')

      Row() {
        if (this.selectedCategory) {
          Text(this.getCategoryIcon(this.selectedCategory.name))
            .fontSize(24)
          Text(this.selectedCategory.name)
            .fontSize(15)
            .fontColor('#333333')
            .margin({ left: 6 })
        } else {
          Text('请选择分类')
            .fontSize(14)
            .fontColor('#BBBBBB')
        }
        Blank()
        Image($r('sys.media.ohos_ic_public_arrow_right'))
          .width(16)
          .height(16)
      }
      .width('100%')
      .height(48)
      .padding({ left: 12, right: 12 })
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
      .onClick(() => { this.showCategoryPanel = !this.showCategoryPanel; })

      // 分类网格
      if (this.showCategoryPanel) {
        Grid() {
          ForEach(this.categories, (cat: Category) => {
            GridItem() {
              Column() {
                Text(this.getCategoryIcon(cat.name))
                  .fontSize(28)
                Text(cat.name)
                  .fontSize(11)
                  .fontColor('#666666')
                  .margin({ top: 2 })
              }
              .width('100%')
              .height(70)
              .justifyContent(FlexAlign.Center)
              .borderRadius(8)
              .backgroundColor(this.selectedCategory?.id === cat.id ? '#E3F2FD' : '#F5F5F5')
            }
            .onClick(() => { this.onCategorySelect(cat); })
          })
        }
        .columnsTemplate('1fr 1fr 1fr 1fr')
        .rowsGap(8)
        .columnsGap(8)
        .width('100%')
        .margin({ top: 8 })
      }
    }
    .width('100%')
    .margin({ top: 20 })
  }

  @Builder
  buildRemarkSection() {
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
      .margin({ bottom: 8 })

      Row() {
        TextInput({ placeholder: '添加备注说明（选填）', text: this.remark })
          .fontSize(14)
          .backgroundColor(Color.Transparent)
          .maxLength(200)
          .layoutWeight(1)
          .onChange((value: string) => { this.remark = value; })

        // AI推荐分类按钮
        Button('🤖 AI推荐')
          .fontSize(11)
          .height(28)
          .backgroundColor('#F3E5F5')
          .fontColor('#9C27B0')
          .borderRadius(14)
          .enabled(!this.isAiClassifying && this.remark.length > 0)
          .padding({ left: 10, right: 10 })
          .margin({ left: 8 })
          .onClick(() => { this.onAiClassify(); })
      }
      .width('100%')
      .height(44)
      .padding({ left: 12, right: 8 })
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
    }
    .width('100%')
    .margin({ top: 20 })
  }

  @Builder
  buildImageSection() {
    Column() {
      Text('小票图片（选填）')
        .fontSize(13)
        .fontColor('#999999')
        .margin({ bottom: 8 })
        .width('100%')

      Row() {
        if (this.receiptImagePath) {
          Text('📷 已选择图片')
            .fontSize(13)
            .fontColor('#4CAF50')
            .layoutWeight(1)
          Text('删除')
            .fontSize(12)
            .fontColor('#FF6B6B')
            .onClick(() => { this.receiptImagePath = ''; })
        } else {
          Text('📷 拍摄或选择小票照片')
            .fontSize(14)
            .fontColor('#4A90D9')
        }
      }
      .width('100%')
      .height(44)
      .padding({ left: 12, right: 12 })
      .backgroundColor('#FFFFFF')
      .borderRadius(12)
      .onClick(() => { this.onPickImage(); })
    }
    .width('100%')
    .margin({ top: 20 })
  }

  @Builder
  buildSubmitButtons() {
    Column() {
      Button(this.isSubmitting ? '提交中...' : '✓  记一笔')
        .width('100%')
        .height(48)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .backgroundColor(this.billType === 'expense' ? '#FF6B6B' : '#4CAF50')
        .borderRadius(24)
        .fontColor('#FFFFFF')
        .enabled(!this.isSubmitting)
        .margin({ top: 30 })
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
          .onClick(() => { this.onCancel(); })
      }
      .width('100%')
      .margin({ top: 12 })
    }
    .width('100%')
  }

  // ==================== 辅助方法 ====================

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
