/**
 * 首页记账核心页
 * 功能：收支类型切换、金额输入、分类选择、备注、拍照上传、草稿保存、离线记账
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import picker from "@ohos.file.picker";
import fs from "@ohos.file.fs";
import { CategoryApi } from "../../network/api/categoryApi";
import { BillApi } from "../../network/api/billApi";
import { FileApi } from "../../network/api/fileApi";
import { AiApi } from "../../network/api/aiApi";
import { AppState } from "../../store/AppState";
import { StorageHelper } from "../../utils/storage";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { Category } from "../../model/CategoryModel";
import { DraftBill, OfflineBill } from "../../model/BillModel";
import { AiClassifyResult } from "../../model/AiModel";
import { EmptyView } from "../../components/common/EmptyView";
import { BillAddRequest } from "../../model/BillModel";

@Entry
@Component
struct IndexPage {
  // ==================== 状态变量 ====================
  @State billType: string = "expense"; // income | expense
  @State amount: string = "";
  @State selectedCategory: Category | null = null;
  @State remark: string = "";
  @State categories: Category[] = [];
  @State isLoading: boolean = false;
  @State isSubmitting: boolean = false;
  @State isOnline: boolean = true;
  @State offlineCount: number = 0;
  @State receiptImagePath: string = "";
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
      console.error("加载分类失败:", JSON.stringify(err));
    }
  }

  async loadDraft(): Promise<void> {
    const draftJson = await StorageHelper.getDraft();
    if (draftJson) {
      try {
        const draft: DraftBill = JSON.parse(draftJson);
        this.billType = draft.type || "expense";
        this.amount = draft.amount || "";
        this.remark = draft.remark || "";
        await this.loadCategories();
        if (draft.categoryId) {
          const cat = this.categories.find((c) => c.id === draft.categoryId);
          if (cat) this.selectedCategory = cat;
        }
      } catch (e) {
        /* ignore */
      }
    }
  }

  async checkOfflineBills(): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    try {
      const bills: OfflineBill[] = JSON.parse(json);
      this.offlineCount = bills.filter((b) => b.syncStatus === "pending").length;
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
      const pending = bills.filter((b) => b.syncStatus === "pending");
      if (pending.length === 0) return;

      const result = await BillApi.syncOfflineBills(pending);
      if (result) {
        for (const r of result.results) {
          const bill = bills.find((b) => b.uuid === r.uuid);
          if (bill) {
            bill.syncStatus = r.status === "success" ? "synced" : "pending";
          }
        }
        const remaining = bills.filter((b) => b.syncStatus !== "synced");
        await StorageHelper.saveOfflineBills(JSON.stringify(remaining));
        this.offlineCount = remaining.filter((b) => b.syncStatus === "pending").length;

        if (result.successCount > 0) {
          ToastUtil.showSuccess(`已自动同步${result.successCount}条离线账单`);
        }
      }
    } catch (e) {
      /* ignore */
    }
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
      ToastUtil.showInfo("请先输入消费备注");
      return;
    }
    this.isAiClassifying = true;
    try {
      const res = await AiApi.classify({
        remark: this.remark,
        type: this.billType,
      });
      if (res.code === 200 && res.data) {
        this.aiClassifyResult = res.data;
        const cat = this.categories.find((c) => c.id === res.data!.categoryId);
        if (cat) {
          this.selectedCategory = cat;
          ToastUtil.showSuccess(
            `AI推荐：${cat.name}（置信度${(res.data.confidence * 100).toFixed(0)}%）`,
          );
        }
      }
    } catch (err) {
      /* ignore */
    } finally {
      this.isAiClassifying = false;
    }
  }

  /**
   * 选择图片（拍照或从相册）
   * <p>P0-3 修复：使用 PhotoViewPicker 真正从相册选择图片，复制到应用沙箱缓存目录。
   */
  async onPickImage(): Promise<void> {
    try {
      // 关键修复（P0-3）：使用 HarmonyOS PhotoViewPicker 从系统相册选择图片
      const photoPicker = new picker.PhotoViewPicker();
      const result = await photoPicker.select({
        MIMEType: picker.PhotoViewMIMETypes.IMAGE_TYPE,
        maxSelectNumber: 1,
      });
      if (!result.photoUris || result.photoUris.length === 0) {
        return; // 用户取消选择
      }
      const srcUri: string = result.photoUris[0];
      // 把选中的图片复制到应用沙箱缓存目录（避免后续访问受限于 photo picker 的临时授权）
      const ctx: Context = getContext(this);
      const cacheDir: string = ctx.cacheDir;
      const fileName: string = `receipt_${Date.now()}.jpg`;
      const destPath: string = `${cacheDir}/${fileName}`;
      const srcFile = fs.openSync(srcUri, fs.OpenMode.READ_ONLY);
      const destFile = fs.openSync(
        destPath,
        fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY | fs.OpenMode.TRUNC,
      );
      fs.copyFileSync(srcFile.fd, destFile.fd);
      fs.closeSync(srcFile);
      fs.closeSync(destFile);
      this.receiptImagePath = destPath;
      ToastUtil.showSuccess("图片已选择");
    } catch (err) {
      console.error("选择图片失败:", JSON.stringify(err));
      ToastUtil.showError("选择图片失败：" + (err?.message || ""));
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
      ToastUtil.showError("请输入有效金额");
      return;
    }
    if (!this.selectedCategory) {
      ToastUtil.showError("请选择消费分类");
      return;
    }

    this.isSubmitting = true;

    try {
      // 上传图片（如有）
      let receiptImage = "";
      if (this.receiptImagePath) {
        const uploadRes = await FileApi.uploadReceipt(this.receiptImagePath, "receipt.jpg");
        if (uploadRes.code === 200 && uploadRes.data) {
          receiptImage = uploadRes.data.filePath;
        }
      }

      const billData: BillAddRequest = {
        amount: amt,
        type: this.billType,
        categoryId: this.selectedCategory.id,
        remark: this.remark,
        receiptImage: receiptImage,
        consumeTime: DateUtil.getCurrentDateTimeStr(),
      };

      const res = await BillApi.add(billData);

      if (res.code === 200) {
        // 联网成功 - 清除草稿
        await StorageHelper.clearDraft();
        ToastUtil.showSuccess("记账成功");
        this.clearForm();
      } else if (res.code === -1) {
        // 网络异常 - 存入离线队列
        await this.saveToOfflineQueue(billData);
      } else {
        ToastUtil.showError(res.message || "记账失败");
      }
    } catch (err) {
      // 异常情况 - 存入离线队列
      const amt = parseFloat(this.amount);
      const offlineData: BillAddRequest = {
        amount: amt,
        type: this.billType,
        categoryId: this.selectedCategory!.id,
        remark: this.remark,
        receiptImage: "",
        consumeTime: DateUtil.getCurrentDateTimeStr(),
      };
      await this.saveToOfflineQueue(offlineData);
    } finally {
      this.isSubmitting = false;
    }
  }

  /**
   * 离线缓存 - 存入Preferences离线队列
   * 最多支持100条待同步数据
   */
  async saveToOfflineQueue(billData: BillAddRequest): Promise<void> {
    try {
      const json = await StorageHelper.getOfflineBills();
      const bills: OfflineBill[] = JSON.parse(json);
      const pendingCount = bills.filter((b) => b.syncStatus === "pending").length;

      if (pendingCount >= 100) {
        ToastUtil.showWarning("离线队列已达上限（100条），请联网同步后再记账");
        return;
      }

      const offlineBill: OfflineBill = {
        uuid: DateUtil.generateUUID(),
        amount: billData.amount,
        type: billData.type,
        categoryId: billData.categoryId,
        categoryName: this.selectedCategory?.name || "",
        remark: billData.remark || "",
        consumeTime: billData.consumeTime,
        createdAt: DateUtil.getCurrentDateTimeStr(),
        offlineCreatedAt: DateUtil.getCurrentDateTimeStr(),
        syncStatus: "pending",
        hasImage: billData.receiptImage ? true : false,
        receiptImage: billData.receiptImage || "",
      };

      bills.push(offlineBill);
      await StorageHelper.saveOfflineBills(JSON.stringify(bills));
      this.offlineCount = bills.filter((b) => b.syncStatus === "pending").length;

      ToastUtil.showSuccess("已离线保存，联网后自动同步");
      this.clearForm();
    } catch (e) {
      ToastUtil.showError("离线保存失败");
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
      categoryName: this.selectedCategory?.name || "",
      remark: this.remark,
      consumeTime: DateUtil.getCurrentDateTimeStr(),
    };
    await StorageHelper.saveDraft(JSON.stringify(draft));
    ToastUtil.showSuccess("草稿已保存");
  }

  /**
   * 取消录入 - 清空表单
   */
  onCancel(): void {
    this.clearForm();
  }

  clearForm(): void {
    this.amount = "";
    this.remark = "";
    this.selectedCategory = null;
    this.receiptImagePath = "";
    this.aiClassifyResult = null;
    StorageHelper.clearDraft();
  }

  build() {
    Column() {
      // 顶部导航
      this.buildTopBar()

      // 滚动内容区
      Scroll() {
        Column() {
          // 收支类型切换
          this.buildTypeSwitch()

          // 金额输入区
          this.buildAmountInput()

          // 快捷金额
          this.buildQuickAmounts()

          // 分类选择
          this.buildCategorySection()

          // 备注输入
          this.buildRemarkSection()

          // 图片选择
          this.buildImageSection()

          // 提交按钮
          this.buildSubmitButtons()
        }
        .padding({ left: 16, right: 16, bottom: 80 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width("100%")
    .height("100%")
    .backgroundColor($r("app.color.bg_page"));
  }

  // ==================== 子组件构建方法 ====================

  @Builder
  buildTopBar() {
    Row() {
      Text("💰 记账")
        .fontSize(20)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
      Blank()

      if (this.offlineCount > 0) {
        Row() {
          Text(`⏳ ${this.offlineCount}待同步`)
            .fontSize(11)
            .fontColor($r("app.color.warning"))
          Image($r("sys.media.ohos_ic_public_arrow_right"))
            .width(14)
            .height(14)
            .fillColor($r("app.color.warning"))
        }
        .padding({ left: 8, right: 8, top: 4, bottom: 4 })
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(12)
        .onClick(() => {
          router.pushUrl({ url: "pages/settings/SettingsPage" });
        });
      }
      Row() {
        Image($r("sys.media.ohos_ic_public_clock"))
          .width(22)
          .height(22)
          .fillColor($r("app.color.text_secondary"))
      }
      .margin({ left: 8 })
      .onClick(() => {
        router.pushUrl({ url: "pages/bill/BillListPage" });
      })
    }
    .width("100%")
    .padding({ left: 16, right: 16, top: 40, bottom: 14 })
    .backgroundColor($r("app.color.bg_card"));
  }

  @Builder
  buildTypeSwitch() {
    Row() {
      Button("📤 支出")
        .fontSize(14)
        .backgroundColor(this.billType === "expense" ? "#FF6B6B" : $r("app.color.bg_page"))
        .fontColor(this.billType === "expense" ? Color.White : $r("app.color.text_hint"))
        .borderRadius(20)
        .height(36)
        .flexGrow(1)
        .onClick(() => {
          this.onTypeChange("expense");
        })
      Button("📥 收入")
        .fontSize(14)
        .backgroundColor(this.billType === "income" ? "#4CAF50" : $r("app.color.bg_page"))
        .fontColor(this.billType === "income" ? Color.White : $r("app.color.text_hint"))
        .borderRadius(20)
        .height(36)
        .flexGrow(1)
        .margin({ left: 10 })
        .onClick(() => {
          this.onTypeChange("income");
        })
    }
    .width("100%")
    .margin({ top: 16 });
  }

  @Builder
  buildAmountInput() {
    Column() {
      Text(this.billType === "expense" ? "消费金额" : "收入金额")
        .fontSize(13)
        .fontColor($r("app.color.text_hint"))
        .margin({ bottom: 10 })
      Row() {
        Text("¥")
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
        TextInput({ placeholder: "0.00", text: this.amount })
          .fontSize(36)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
          .type(InputType.Number)
          .backgroundColor(Color.Transparent)
          .placeholderColor($r("app.color.text_hint"))
          .layoutWeight(1)
          .onChange((value: string) => {
            this.amount = value;
          })
      }
      .width("100%")
      .padding({ left: 12, right: 12 })
      .height(70)
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
    }
    .width("100%")
    .margin({ top: 20 });
  }

  @Builder
  buildQuickAmounts() {
    Row() {
      ForEach(this.quickAmounts, (amt: number) => {
        Button(`¥${amt}`)
          .fontSize(13)
          .backgroundColor(
            this.amount === amt.toString() ? $r("app.color.primary") : $r("app.color.bg_page"),
          )
          .fontColor(this.amount === amt.toString() ? Color.White : $r("app.color.text_secondary"))
          .borderRadius(16)
          .height(32)
          .padding({ left: 12, right: 12 })
          .margin({ right: 8 })
          .onClick(() => {
            this.onQuickAmount(amt);
          });
      })
    }
    .width("100%")
    .margin({ top: 12 });
  }

  @Builder
  buildCategorySection() {
    Column() {
      Text("消费分类")
        .fontSize(13)
        .fontColor($r("app.color.text_hint"))
        .margin({ bottom: 10 })
        .width("100%")
      Row() {
        if (this.selectedCategory) {
          Text(IndexPage.getCategoryIcon(this.selectedCategory.name)).fontSize(24);
          Text(this.selectedCategory.name)
            .fontSize(15)
            .fontColor($r("app.color.text_primary"))
            .margin({ left: 6 });
        } else {
          Text("请选择分类").fontSize(14).fontColor($r("app.color.text_hint"));
        }
        Blank()
        Image($r("sys.media.ohos_ic_public_arrow_right"))
          .width(16)
          .height(16)
      }
      .width("100%")
      .height(48)
      .padding({ left: 12, right: 12 })
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
      .onClick(() => {
        this.showCategoryPanel = !this.showCategoryPanel;
      })

      // 分类网格
      if (this.showCategoryPanel) {
        Grid() {
          ForEach(this.categories, (cat: Category) => {
            GridItem() {
              Column() {
                Text(IndexPage.getCategoryIcon(cat.name)).fontSize(28)
                Text(cat.name)
                  .fontSize(11)
                  .fontColor($r("app.color.text_secondary"))
                  .margin({ top: 2 })
              }
              .width("100%")
              .height(70)
              .justifyContent(FlexAlign.Center)
              .borderRadius(8)
              .backgroundColor(
                this.selectedCategory?.id === cat.id
                  ? $r("app.color.bg_page")
                  : $r("app.color.bg_page"),
              )
            }
            .onClick(() => {
              this.onCategorySelect(cat);
            });
          })
        }
        .columnsTemplate("1fr 1fr 1fr 1fr")
        .rowsGap(8)
        .columnsGap(8)
        .width("100%")
        .margin({ top: 8 });
      }
    }
    .width("100%")
    .margin({ top: 20 });
  }

  @Builder
  buildRemarkSection() {
    Column() {
      Row() {
        Text("备注")
          .fontSize(13)
          .fontColor($r("app.color.text_hint"))
        Blank()
        Text(`${this.remark.length}/200`)
          .fontSize(11)
          .fontColor($r("app.color.text_hint"))
      }
      .width("100%")
      .margin({ bottom: 8 })
      Row() {
        TextInput({ placeholder: "添加备注说明（选填）", text: this.remark })
          .fontSize(14)
          .backgroundColor(Color.Transparent)
          .maxLength(200)
          .layoutWeight(1)
          .onChange((value: string) => {
            this.remark = value;
          })

        // AI推荐分类按钮
        Button("🤖 AI推荐")
          .fontSize(11)
          .height(28)
          .backgroundColor($r("app.color.bg_page"))
          .fontColor($r("app.color.primary"))
          .borderRadius(14)
          .enabled(!this.isAiClassifying && this.remark.length > 0)
          .padding({ left: 10, right: 10 })
          .margin({ left: 8 })
          .onClick(() => {
            this.onAiClassify();
          })
      }
      .width("100%")
      .height(44)
      .padding({ left: 12, right: 8 })
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
    }
    .width("100%")
    .margin({ top: 20 });
  }

  @Builder
  buildImageSection() {
    Column() {
      Text("小票图片（选填）")
        .fontSize(13)
        .fontColor($r("app.color.text_hint"))
        .margin({ bottom: 8 })
        .width("100%")
      Row() {
        if (this.receiptImagePath) {
          Text("📷 已选择图片").fontSize(13).fontColor($r("app.color.success")).layoutWeight(1);
          Text("删除")
            .fontSize(12)
            .fontColor($r("app.color.danger"))
            .onClick(() => {
              this.receiptImagePath = "";
            });
        } else {
          Text("📷 拍摄或选择小票照片").fontSize(14).fontColor($r("app.color.primary"));
        }
      }
      .width("100%")
      .height(44)
      .padding({ left: 12, right: 12 })
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
      .onClick(() => {
        this.onPickImage();
      })
    }
    .width("100%")
    .margin({ top: 20 });
  }

  @Builder
  buildSubmitButtons() {
    Column() {
      Button(this.isSubmitting ? "提交中..." : "✓  记一笔")
        .width("100%")
        .height(48)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .backgroundColor(this.billType === "expense" ? "#FF6B6B" : "#4CAF50")
        .borderRadius(24)
        .fontColor("#FFFFFF")
        .enabled(!this.isSubmitting)
        .margin({ top: 30 })
        .onClick(() => {
          this.onSubmit();
        })
      Row() {
        Button("保存草稿")
          .fontSize(13)
          .backgroundColor("#F0F0F0")
          .fontColor("#666666")
          .borderRadius(20)
          .height(38)
          .flexGrow(1)
          .onClick(() => {
            this.onSaveDraft();
          })
        Button("取消")
          .fontSize(13)
          .backgroundColor("#F0F0F0")
          .fontColor("#666666")
          .borderRadius(20)
          .height(38)
          .flexGrow(1)
          .margin({ left: 10 })
          .onClick(() => {
            this.onCancel();
          })
      }
      .width("100%")
      .margin({ top: 12 })
    }.width("100%");
  }

  // ==================== 辅助方法 ====================

  private static getCategoryIcon(name: string): string {
    switch (name) {
      case "餐饮":
        return "🍽️";
      case "交通":
        return "🚗";
      case "购物":
        return "🛒";
      case "娱乐":
        return "🎮";
      case "住房":
        return "🏠";
      case "医疗":
        return "🏥";
      case "教育":
        return "📚";
      case "通讯":
        return "📱";
      case "服饰":
        return "👔";
      case "日用品":
        return "🧴";
      case "丽人":
        return "💄";
      case "运动":
        return "⚽";
      case "旅行":
        return "✈️";
      case "宠物":
        return "🐱";
      case "数码":
        return "💻";
      case "其他支出":
        return "📌";
      case "工资":
        return "💰";
      case "奖金":
        return "🏆";
      case "投资收益":
        return "📈";
      case "兼职":
        return "💼";
      case "红包":
        return "🧧";
      case "退款":
        return "↩️";
      case "其他收入":
        return "📌";
      default:
        return "📋";
    }
  }
}
