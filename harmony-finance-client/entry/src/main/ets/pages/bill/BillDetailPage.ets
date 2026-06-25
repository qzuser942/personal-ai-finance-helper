/**
 * 账单详情/编辑页
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import picker from "@ohos.file.picker";
import fs from "@ohos.file.fs";
import { BillApi } from "../../network/api/billApi";
import { CategoryApi } from "../../network/api/categoryApi";
import { FileApi } from "../../network/api/fileApi";
import { BillDetail, BillAddRequest } from "../../model/BillModel";
import { Category } from "../../model/CategoryModel";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { ValidationUtil } from "../../utils/ValidationUtil";

@Entry
@Component
struct BillDetailPage {
  @State bill: BillDetail | null = null;
  @State categories: Category[] = [];
  @State isLoading: boolean = true;
  @State isEditing: boolean = false;

  // 编辑表单
  @State editAmount: string = "";
  @State editType: string = "";
  @State editCategoryId: number = 0;
  @State editRemark: string = "";
  @State editConsumeTime: string = "";
  @State editAmountError: string = "";
  @State editReceiptPath: string = "";

  private billId: number = 0;

  async aboutToAppear(): Promise<void> {
    const params = router.getParams() as Record<string, number>;
    this.billId = params?.["billId"] || 0;
    await this.loadData();
  }

  async loadData(): Promise<void> {
    this.isLoading = true;
    const billRes = await BillApi.getDetail(this.billId);
    const catRes = await CategoryApi.getList();

    if (billRes.code === 200 && billRes.data) {
      this.bill = billRes.data;
    }
    if (catRes.code === 200 && catRes.data) {
      this.categories = catRes.data;
    }
    this.isLoading = false;
  }

  onEdit(): void {
    if (!this.bill) return;
    this.editAmount = this.bill.amount.toString();
    this.editType = this.bill.type;
    this.editCategoryId = this.bill.categoryId;
    this.editRemark = this.bill.remark || "";
    this.editConsumeTime = this.bill.consumeTime;
    this.editReceiptPath = "";
    this.isEditing = true;
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
      this.editReceiptPath = destPath;
      ToastUtil.showSuccess("图片已选择");
    } catch (err) {
      console.error("选择图片失败:", JSON.stringify(err));
    }
  }

  async onSaveEdit(): Promise<void> {
    if (this.editAmountError !== "") {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
    const amt = parseFloat(this.editAmount);
    if (!amt || amt <= 0) {
      ToastUtil.showError("请输入有效金额");
      return;
    }

    // 上传新图片（如有）
    let receiptImage = "";
    if (this.editReceiptPath) {
      const uploadRes = await FileApi.uploadReceipt(this.editReceiptPath, "receipt.jpg");
      if (uploadRes.code === 200 && uploadRes.data) {
        receiptImage = uploadRes.data.filePath;
      }
    } else if (this.bill && this.bill.receiptImage) {
      receiptImage = this.bill.receiptImage;
    }

    const res = await BillApi.update(this.billId, {
      amount: amt,
      categoryId: this.editCategoryId,
      remark: this.editRemark,
      receiptImage: receiptImage,
      consumeTime: this.editConsumeTime,
    });

    if (res.code === 200) {
      ToastUtil.showSuccess("修改成功");
      this.isEditing = false;
      await this.loadData();
    } else {
      ToastUtil.showError(res.message || "修改失败");
    }
  }

  async onDelete(): Promise<void> {
    const res = await BillApi.delete(this.billId);
    if (res.code === 200) {
      ToastUtil.showSuccess("已删除");
      router.back();
    } else {
      ToastUtil.showError(res.message || "删除失败");
    }
  }

  build() {
    Column() {
      // 顶部导航
      Row() {
        Image($r("sys.media.ohos_ic_public_arrow_left"))
          .width(24)
          .height(24)
          .onClick(() => {
            router.back();
          })
        Text("账单详情")
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
          .margin({ left: 8 })
        Blank()
        if (!this.isEditing) {
          Text("编辑")
            .fontSize(14)
            .fontColor($r("app.color.primary"))
            .onClick(() => {
              this.onEdit();
            });
        }
      }
      .width("100%")
      .padding({ left: 16, right: 16, top: 40, bottom: 10 })
      .backgroundColor($r("app.color.bg_card"))

      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color($r("app.color.primary"))
          Text("加载中...")
            .fontSize(14)
            .fontColor($r("app.color.text_hint"))
            .margin({ top: 12 })
        }
        .width("100%")
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center);
      } else if (this.bill) {
        Scroll() {
          Column() {
            if (this.isEditing) {
              this.buildEditForm();
            } else {
              this.buildDetailView();
            }
          }
          .padding({ left: 16, right: 16, bottom: 30 })
        }.layoutWeight(1);
      }
    }
    .width("100%")
    .height("100%")
    .backgroundColor($r("app.color.bg_page"));
  }

  @Builder
  buildDetailView() {
    Column() {
      // 金额卡片
      Column() {
        Text(this.bill!.type === "income" ? "收入金额" : "支出金额")
          .fontSize(12)
          .fontColor(Color.White)
          .opacity(0.8)
        Text(`${this.bill!.type === "income" ? "+" : "-"}${this.bill!.amount.toFixed(2)}`)
          .fontSize(40)
          .fontWeight(FontWeight.Bold)
          .fontColor(Color.White)
          .margin({ top: 4 })
      }
      .width("100%")
      .padding(24)
      .borderRadius(16)
      .backgroundColor(
        this.bill!.type === "income" ? $r("app.color.success") : $r("app.color.danger"),
      )

      // 详情字段
      this.buildDetailItem("收支类型", this.bill!.type === "income" ? "💰 收入" : "💸 支出")
      this.buildDetailItem("消费分类", this.bill!.categoryName)
      this.buildDetailItem("备注", this.bill!.remark || "无")
      this.buildDetailItem("消费时间", this.bill!.consumeTime)
      this.buildDetailItem("创建时间", this.bill!.createdAt)

      if (this.bill!.receiptImage) {
        this.buildDetailItem("小票图片", "");
        Image(FileApi.getImageUrl(this.bill!.receiptImage))
          .width("100%")
          .objectFit(ImageFit.Contain)
          .borderRadius(8)
          .margin({ top: 8 });
      }

      // 操作按钮
      Row() {
        Button("🗑 删除")
          .fontSize(14)
          .backgroundColor($r("app.color.danger"))
          .fontColor(Color.White)
          .borderRadius(20)
          .height(42)
          .flexGrow(1)
          .onClick(() => {
            this.onDelete();
          })
      }
      .width("100%")
      .margin({ top: 30 })
    };
  }

  @Builder
  buildEditForm() {
    Column() {
      // 金额
      this.buildEditLabel("金额")
      TextInput({ text: this.editAmount })
        .type(InputType.Normal)
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .height(48)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(8)
        .width("100%")
        .padding({ left: 12, right: 12 })
        .onChange((v: string) => {
          const hasNonNumeric = ValidationUtil.hasNonNumericChar(v);
          const filtered = ValidationUtil.filterAmountInput(v);
          this.editAmount = filtered;
          if (hasNonNumeric) {
            this.editAmountError = "⚠️警告：请输入数字";
          } else {
            this.editAmountError = ValidationUtil.validateAmount(filtered);
          }
        })

      if (this.editAmountError) {
        Text(this.editAmountError)
          .fontSize(12)
          .fontColor("#FF9800")
          .width("100%")
          .margin({ top: 4, left: 4 });
      }

      // 分类
      this.buildEditLabel("分类")
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
            .height(60)
            .justifyContent(FlexAlign.Center)
            .borderRadius(8)
            .backgroundColor(
              this.editCategoryId === cat.id ? $r("app.color.bg_card") : $r("app.color.bg_page"),
            )
          }
          .onClick(() => {
            this.editCategoryId = cat.id;
          });
        })
      }
      .columnsTemplate("1fr 1fr 1fr 1fr")
      .rowsGap(6)
      .columnsGap(6)
      .width("100%")
      .margin({ top: 4 })

      // 备注
      this.buildEditLabel("备注")
      TextInput({ text: this.editRemark })
        .fontSize(14)
        .height(44)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(8)
        .width("100%")
        .padding({ left: 12, right: 12 })
        .maxLength(500)
        .onChange((v: string) => {
          this.editRemark = v;
        })

      // 小票照片
      this.buildEditLabel("小票凭证")
      if (this.editReceiptPath) {
        Row() {
          Image(this.editReceiptPath)
            .width(80)
            .height(80)
            .borderRadius(8)
            .objectFit(ImageFit.Cover)
          Column() {
            Text("已选择新图片")
              .fontSize(12)
              .fontColor($r("app.color.success"))
              .margin({ bottom: 2 })
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
              this.editReceiptPath = "";
            })
        }
        .width("100%")
        .padding(10)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(8)
        .onClick(() => {
          this.onPickImage();
        });
      } else if (this.bill && this.bill.receiptImage) {
        Row() {
          Image(FileApi.getImageUrl(this.bill.receiptImage))
            .width(80)
            .height(80)
            .borderRadius(8)
            .objectFit(ImageFit.Cover)
          Column() {
            Text("当前小票")
              .fontSize(12)
              .fontColor($r("app.color.text_hint"))
              .margin({ bottom: 2 })
            Text("点击替换新图片")
              .fontSize(11)
              .fontColor($r("app.color.primary"))
          }
          .alignItems(HorizontalAlign.Start)
          .margin({ left: 10 })
          .layoutWeight(1)
        }
        .width("100%")
        .padding(10)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(8)
        .onClick(() => {
          this.onPickImage();
        });
      } else {
        Button("📷 拍摄或选择小票照片")
          .fontSize(14)
          .backgroundColor($r("app.color.bg_card"))
          .fontColor($r("app.color.primary"))
          .borderRadius(8)
          .height(42)
          .width("100%")
          .onClick(() => {
            this.onPickImage();
          });
      }

      // 时间
      this.buildEditLabel("消费时间")
      TextInput({ text: this.editConsumeTime })
        .fontSize(14)
        .height(44)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(8)
        .width("100%")
        .padding({ left: 12, right: 12 })
        .onChange((v: string) => {
          this.editConsumeTime = v;
        })

      // 按钮
      Row() {
        Button("取消")
          .fontSize(14)
          .backgroundColor($r("app.color.bg_page"))
          .fontColor($r("app.color.text_secondary"))
          .borderRadius(20)
          .height(42)
          .flexGrow(1)
          .onClick(() => {
            this.isEditing = false;
          })
        Button("保存")
          .fontSize(14)
          .backgroundColor($r("app.color.primary"))
          .fontColor(Color.White)
          .borderRadius(20)
          .height(42)
          .flexGrow(1)
          .margin({ left: 10 })
          .onClick(() => {
            this.onSaveEdit();
          })
      }
      .width("100%")
      .margin({ top: 20 })
    };
  }

  @Builder
  buildDetailItem(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(14)
        .fontColor($r("app.color.text_hint"))
        .width(70)
      Text(value)
        .fontSize(14)
        .fontColor($r("app.color.text_primary"))
        .layoutWeight(1)
    }
    .width("100%")
    .padding({ top: 14, bottom: 14, left: 16, right: 16 })
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(8)
    .margin({ top: 10 });
  }

  @Builder
  buildEditLabel(label: string) {
    Text(label)
      .fontSize(12)
      .fontColor($r("app.color.text_hint"))
      .width("100%")
      .margin({ top: 14, bottom: 6 });
  }

  private getCategoryIcon(name: string): string {
    const map: Map<string, string> = new Map([
      ["餐饮", "🍽️"],
      ["交通", "🚗"],
      ["购物", "🛒"],
      ["娱乐", "🎮"],
      ["住房", "🏠"],
      ["医疗", "🏥"],
      ["教育", "📚"],
      ["通讯", "📱"],
      ["服饰", "👔"],
      ["日用品", "🧴"],
      ["丽人", "💄"],
      ["运动", "⚽"],
      ["旅行", "✈️"],
      ["宠物", "🐱"],
      ["数码", "💻"],
      ["其他支出", "📌"],
      ["工资", "💰"],
      ["奖金", "🏆"],
      ["投资收益", "📈"],
      ["兼职", "💼"],
      ["红包", "🧧"],
      ["退款", "↩️"],
      ["其他收入", "📌"],
    ]);
    return map.get(name) || "📋";
  }
}
