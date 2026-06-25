/**
 * 主页 - 底部Tab导航容器
 * 包含：记账(内联)、账单、统计、目标、AI、设置
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import picker from "@ohos.file.picker";
import fs from "@ohos.file.fs";
import { CategoryApi } from "../../network/api/categoryApi";
import { BillApi } from "../../network/api/billApi";
import { FileApi } from "../../network/api/fileApi";
import { StorageHelper, KEYS } from "../../utils/storage";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { ValidationUtil } from "../../utils/ValidationUtil";
import { Category } from "../../model/CategoryModel";
import { OfflineBill, BillAddRequest, DraftBill } from "../../model/BillModel";

@Entry
@Component
struct MainPage {
  @State currentTab: number = 0;

  private readonly TAB_TITLES: string[] = ["记账", "账单", "统计", "目标", "AI", "设置"];
  private readonly TAB_ICONS: string[] = ["💰", "📋", "📊", "🎯", "🤖", "⚙️"];

  build() {
    Tabs({ barPosition: BarPosition.End, index: this.currentTab }) {
      TabContent() {
        RecordPage()
      }.tabBar(this.tabBuilder(0))
      TabContent() {
        PlaceHolderView({ message: "账单流水", icon: "📋" })
      }.tabBar(this.tabBuilder(1))
      TabContent() {
        PlaceHolderView({ message: "月度统计", icon: "📊" })
      }.tabBar(this.tabBuilder(2))
      TabContent() {
        PlaceHolderView({ message: "预算与存钱目标", icon: "🎯" })
      }.tabBar(this.tabBuilder(3))
      TabContent() {
        PlaceHolderView({ message: "AI理财分析", icon: "🤖" })
      }.tabBar(this.tabBuilder(4))
      TabContent() {
        PlaceHolderView({ message: "设置", icon: "⚙️" })
      }.tabBar(this.tabBuilder(5))
    }
    .barHeight(56)
    .barBackgroundColor($r("app.color.bg_card"))
    .animationDuration(0)
    .scrollable(false)
    .onChange((index: number) => {
      // P1-4 修复：避免 Tab 闪烁
      // 1) 立即更新 currentTab 状态（控制高亮）
      this.currentTab = index;
      // 2) 跳页（不重置 currentTab，避免 PlaceHolderView 闪现）
      if (index !== 0) {
        this.navigateToTab(index);
      }
    })
    .width("100%")
    .height("100%");
  }

  @Builder
  tabBuilder(index: number) {
    Column() {
      Text(this.TAB_ICONS[index])
        .fontSize(20)
        .margin({ bottom: 1 })
      Text(this.TAB_TITLES[index])
        .fontSize(10)
        .fontColor(this.currentTab === index ? $r("app.color.primary") : $r("app.color.text_hint"))
    }
    .width("100%")
    .padding({ top: 4, bottom: 4 })
    .alignItems(HorizontalAlign.Center);
  }

  navigateToTab(index: number): void {
    const routes: Record<number, string> = {
      1: "pages/bill/BillListPage",
      2: "pages/statistics/StatisticsPage",
      3: "pages/budget/BudgetPage",
      4: "pages/ai/AiAnalysisPage",
      5: "pages/settings/SettingsPage",
    };

    if (routes[index]) {
      // P1-4 修复：跳页后不重置 currentTab，避免 PlaceHolderView 闪现
      router.pushUrl({ url: routes[index] });
    }
  }
}

/**
 * 占位视图 - 用于非记账Tab
 */
@Component
struct PlaceHolderView {
  @Prop message: string = "";
  @Prop icon: string = "";

  build() {
    Column() {
      Text(this.icon)
        .fontSize(48)
        .opacity(0.3)
      Text(this.message)
        .fontSize(14)
        .fontColor($r("app.color.text_hint"))
        .margin({ top: 8 })
    }
    .width("100%")
    .height("100%")
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor($r("app.color.bg_page"));
  }
}

/**
 * 记账页核心组件
 */
@Component
struct RecordPage {
  @State billType: string = "expense";
  @State amount: string = "";
  @State selectedCategory: Category | null = null;
  @State remark: string = "";
  @State categories: Category[] = [];
  @State showCategoryPanel: boolean = false;
  @State offlineCount: number = 0;
  @State isSubmitting: boolean = false;
  @State amountError: string = "";
  @State receiptImagePath: string = "";
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
      console.error("加载分类失败:", JSON.stringify(e));
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

  async onSubmit(): Promise<void> {
    if (this.amountError !== "") {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
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

    let billData: BillAddRequest = {
      amount: amt,
      type: this.billType,
      categoryId: this.selectedCategory.id,
      remark: this.remark,
      receiptImage: "",
      consumeTime: DateUtil.getCurrentDateTimeStr(),
    };

    try {
      // 上传图片（如有）
      if (this.receiptImagePath) {
        const uploadRes = await FileApi.uploadReceipt(this.receiptImagePath, "receipt.jpg");
        if (uploadRes.code === 200 && uploadRes.data) {
          billData.receiptImage = uploadRes.data.filePath;
        }
      }

      const res = await BillApi.add(billData);
      if (res.code === 200) {
        ToastUtil.showSuccess("记账成功");
        this.clearForm();
        await this.checkOffline();
      } else if (res.code === -1) {
        await this.saveOffline(billData);
      } else {
        ToastUtil.showError(res.message || "记账失败");
      }
    } catch (e) {
      await this.saveOffline(billData);
    }
    this.isSubmitting = false;
  }

  async saveOffline(billData: BillAddRequest): Promise<void> {
    const json = await StorageHelper.getOfflineBills();
    const bills: OfflineBill[] = JSON.parse(json);
    const pendingCount = bills.filter((b) => b.syncStatus === "pending").length;

    if (pendingCount >= 100) {
      ToastUtil.showWarning("离线队列已达上限（100条），请联网同步后再记账");
      return;
    }

    const offline: OfflineBill = {
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
      hasImage: this.receiptImagePath !== "",
      receiptImage: this.receiptImagePath || "",
    };

    bills.push(offline);
    await StorageHelper.saveOfflineBills(JSON.stringify(bills));
    this.offlineCount = bills.filter((b) => b.syncStatus === "pending").length;
    ToastUtil.showSuccess("已离线保存，联网后自动同步");
    this.clearForm();
  }

  clearForm(): void {
    this.amount = "";
    this.remark = "";
    this.selectedCategory = null;
    this.amountError = "";
    this.receiptImagePath = "";
  }

  onTypeChange(type: string): void {
    this.billType = type;
    this.selectedCategory = null;
    this.loadCategories();
  }

  async onPickImage(): Promise<void> {
    try {
      const photoPicker = new picker.PhotoViewPicker();
      const result = await photoPicker.select({
        MIMEType: picker.PhotoViewMIMETypes.IMAGE_TYPE,
        maxSelectNumber: 1,
      });
      if (!result.photoUris || result.photoUris.length === 0) {
        return;
      }
      const srcUri: string = result.photoUris[0];
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
    }
  }

  build() {
    Column() {
      // 顶部栏
      Row() {
        Text("💰 智能理财")
          .fontSize(20)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
        Blank()

        if (this.offlineCount > 0) {
          Row() {
            Text(`⏳ ${this.offlineCount}待同步`)
              .fontSize(11)
              .fontColor($r("app.color.warning"))
          }
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
          .backgroundColor($r("app.color.bg_card"))
          .borderRadius(12);
        }
      }
      .width("100%")
      .padding({ left: 16, right: 16, top: 40, bottom: 10 })
      .backgroundColor($r("app.color.bg_card"))
      Scroll() {
        Column() {
          // 收支类型切换
          Row() {
            Button("📤 支出")
              .fontSize(14)
              .backgroundColor(
                this.billType === "expense" ? $r("app.color.danger") : $r("app.color.bg_page"),
              )
              .fontColor(this.billType === "expense" ? Color.White : $r("app.color.text_hint"))
              .borderRadius(20)
              .height(36)
              .flexGrow(1)
              .onClick(() => {
                this.onTypeChange("expense");
              })
            Button("📥 收入")
              .fontSize(14)
              .backgroundColor(
                this.billType === "income" ? $r("app.color.success") : $r("app.color.bg_page"),
              )
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
          .margin({ top: 14 })

          // 金额输入
          Column() {
            Text(this.billType === "expense" ? "消费金额" : "收入金额")
              .fontSize(13)
              .fontColor($r("app.color.text_hint"))
              .margin({ bottom: 8 })
            Row() {
              Text("¥")
                .fontSize(28)
                .fontWeight(FontWeight.Bold)
                .fontColor($r("app.color.text_primary"))
              TextInput({ placeholder: "0.00", text: this.amount })
                .fontSize(32)
                .fontWeight(FontWeight.Bold)
                .fontColor($r("app.color.text_primary"))
                .type(InputType.Normal)
                .backgroundColor(Color.Transparent)
                .layoutWeight(1)
                .onChange((v: string) => {
                  const hasNonNumeric = ValidationUtil.hasNonNumericChar(v);
                  const filtered = ValidationUtil.filterAmountInput(v);
                  this.amount = filtered;
                  if (hasNonNumeric) {
                    this.amountError = "⚠️警告：请输入数字";
                  } else {
                    this.amountError = ValidationUtil.validateAmount(filtered);
                  }
                })
            }
            .width("100%")
            .height(65)
            .padding({ left: 12, right: 12 })
            .backgroundColor($r("app.color.bg_card"))
            .borderRadius(12)

            if (this.amountError) {
              Text(this.amountError)
                .fontSize(12)
                .fontColor("#FF9800")
                .width("100%")
                .margin({ top: 6, left: 4 });
            }
          }
          .width("100%")
          .margin({ top: 16 })

          // 快捷金额
          Row() {
            ForEach(this.quickAmounts, (amt: number) => {
              Button(`¥${amt}`)
                .fontSize(13)
                .backgroundColor(
                  this.amount === amt.toString()
                    ? $r("app.color.primary")
                    : $r("app.color.bg_page"),
                )
                .fontColor(
                  this.amount === amt.toString() ? Color.White : $r("app.color.text_secondary"),
                )
                .borderRadius(16)
                .height(30)
                .padding({ left: 12, right: 12 })
                .margin({ right: 8 })
                .onClick(() => {
                  this.amount = amt.toString();
                  this.amountError = "";
                });
            })
          }
          .width("100%")
          .margin({ top: 10 })

          // 分类选择
          Column() {
            Text("消费分类")
              .fontSize(13)
              .fontColor($r("app.color.text_hint"))
              .margin({ bottom: 8 })
              .width("100%")
            Row() {
              Text(this.selectedCategory ? this.selectedCategory.name : "请选择分类")
                .fontSize(14)
                .fontColor(
                  this.selectedCategory ? $r("app.color.text_primary") : $r("app.color.text_hint"),
                )
                .layoutWeight(1)
              Image($r("sys.media.ohos_ic_public_arrow_down"))
                .width(16)
                .height(16)
            }
            .width("100%")
            .height(46)
            .padding({ left: 12, right: 12 })
            .backgroundColor($r("app.color.bg_card"))
            .borderRadius(12)
            .onClick(() => {
              this.showCategoryPanel = !this.showCategoryPanel;
            })

            if (this.showCategoryPanel) {
              Grid() {
                ForEach(this.categories, (cat: Category) => {
                  GridItem() {
                    Column() {
                      Text(this.getCategoryIcon(cat.name)).fontSize(24)
                      Text(cat.name)
                        .fontSize(11)
                        .fontColor($r("app.color.text_secondary"))
                        .margin({ top: 2 })
                    }
                    .width("100%")
                    .height(65)
                    .justifyContent(FlexAlign.Center)
                    .borderRadius(8)
                    .backgroundColor(
                      this.selectedCategory?.id === cat.id
                        ? $r("app.color.bg_page")
                        : $r("app.color.bg_page"),
                    )
                  }
                  .onClick(() => {
                    this.selectedCategory = cat;
                    this.showCategoryPanel = false;
                  });
                })
              }
              .columnsTemplate("1fr 1fr 1fr 1fr")
              .rowsGap(6)
              .columnsGap(6)
              .width("100%")
              .margin({ top: 6 });
            }
          }
          .width("100%")
          .margin({ top: 16 })

          // 备注输入
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
            .margin({ bottom: 6 })
            TextInput({ placeholder: "添加备注说明（选填）", text: this.remark })
              .fontSize(14)
              .height(42)
              .backgroundColor($r("app.color.bg_card"))
              .borderRadius(12)
              .width("100%")
              .padding({ left: 12, right: 12 })
              .maxLength(200)
              .onChange((v: string) => {
                this.remark = v;
              })
          }
          .width("100%")
          .margin({ top: 16 })

          // 小票拍照/上传
          Column() {
            Text("小票凭证")
              .fontSize(13)
              .fontColor($r("app.color.text_hint"))
              .margin({ bottom: 8 })
              .width("100%")

            if (this.receiptImagePath) {
              Row() {
                Image(this.receiptImagePath)
                  .width(80)
                  .height(80)
                  .borderRadius(8)
                  .objectFit(ImageFit.Cover)
                Column() {
                  Text("小票已选择")
                    .fontSize(13)
                    .fontColor($r("app.color.success"))
                    .margin({ bottom: 4 })
                  Text("点击重新选择")
                    .fontSize(11)
                    .fontColor($r("app.color.primary"))
                }
                .alignItems(HorizontalAlign.Start)
                .margin({ left: 10 })
                .layoutWeight(1)
                Blank()
                Button("删除")
                  .fontSize(11)
                  .backgroundColor($r("app.color.bg_page"))
                  .fontColor($r("app.color.text_hint"))
                  .borderRadius(12)
                  .height(28)
                  .onClick(() => {
                    this.receiptImagePath = "";
                  })
              }
              .width("100%")
              .padding(10)
              .backgroundColor($r("app.color.bg_card"))
              .borderRadius(12)
              .onClick(() => {
                this.onPickImage();
              });
            } else {
              Button("📷 拍摄或选择小票照片")
                .fontSize(14)
                .backgroundColor($r("app.color.bg_card"))
                .fontColor($r("app.color.primary"))
                .borderRadius(12)
                .height(46)
                .width("100%")
                .onClick(() => {
                  this.onPickImage();
                });
            }
          }
          .width("100%")
          .margin({ top: 16 })

          // 提交按钮
          Button(this.isSubmitting ? "提交中..." : "✓  记一笔")
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .backgroundColor(
              this.billType === "expense" ? $r("app.color.danger") : $r("app.color.success"),
            )
            .fontColor(Color.White)
            .borderRadius(24)
            .height(48)
            .width("100%")
            .enabled(!this.isSubmitting)
            .margin({ top: 28 })
            .onClick(() => {
              this.onSubmit();
            })
          Row() {
            Button("保存草稿")
              .fontSize(13)
              .backgroundColor($r("app.color.bg_page"))
              .fontColor($r("app.color.text_secondary"))
              .borderRadius(20)
              .height(38)
              .flexGrow(1)
              .onClick(() => {
                this.onSaveDraft();
              })
            Button("取消")
              .fontSize(13)
              .backgroundColor($r("app.color.bg_page"))
              .fontColor($r("app.color.text_secondary"))
              .borderRadius(20)
              .height(38)
              .flexGrow(1)
              .margin({ left: 10 })
              .onClick(() => {
                this.clearForm();
              })
          }
          .width("100%")
          .margin({ top: 10 })
        }
        .padding({ left: 16, right: 16, bottom: 100 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width("100%")
    .height("100%")
    .backgroundColor($r("app.color.bg_page"));
  }

  async onSaveDraft(): Promise<void> {
    if (this.amountError !== "") {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
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

  private getCategoryIcon(name: string): string {
    const map: Record<string, string> = {};
    map["餐饮"] = "🍽️";
    map["交通"] = "🚗";
    map["购物"] = "🛒";
    map["娱乐"] = "🎮";
    map["住房"] = "🏠";
    map["医疗"] = "🏥";
    map["教育"] = "📚";
    map["通讯"] = "📱";
    map["服饰"] = "👔";
    map["日用品"] = "🧴";
    map["丽人"] = "💄";
    map["运动"] = "⚽";
    map["旅行"] = "✈️";
    map["宠物"] = "🐱";
    map["数码"] = "💻";
    map["其他支出"] = "📌";
    map["工资"] = "💰";
    map["奖金"] = "🏆";
    map["投资收益"] = "📈";
    map["兼职"] = "💼";
    map["红包"] = "🧧";
    map["退款"] = "↩️";
    map["其他收入"] = "📌";
    return map[name] || "📋";
  }
}
