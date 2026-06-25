/**
 * 预算与存钱目标页面
 * 功能：月度预算展示、预算预警、存钱目标创建/追踪/归档
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { BudgetApi } from "../../network/api/budgetApi";
import { SaveTargetApi } from "../../network/api/saveTargetApi";
import { CategoryApi } from "../../network/api/categoryApi";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { ValidationUtil } from "../../utils/ValidationUtil";
import { BudgetData, BudgetCreateRequest } from "../../model/BudgetModel";
import { SaveTarget } from "../../model/SaveTargetModel";
import { Category } from "../../model/CategoryModel";
import { BudgetProgress } from "../../components/budget/BudgetProgress";
import { EmptyView } from "../../components/common/EmptyView";

/** 分类预算渲染数据 */
interface CategoryBudgetRowData {
  categoryId: number;
  categoryName: string;
  budgetAmount: number;
  spentAmount: number;
  alertLevel: string;
}

@Entry
@Component
struct BudgetPage {
  // 预算
  @State budget: BudgetData | null = null;
  @State categories: Category[] = [];
  @State showBudgetSetting: boolean = false;
  @State budgetTotalInput: string = "";
  @State categoryBudgetInputs: Record<string, string> = {};
  @State categoryBudgetRows: CategoryBudgetRowData[] = [];
  @State budgetTotalError: string = "";
  @State categoryBudgetErrors: Record<string, string> = {};

  // 存钱目标
  @State targets: SaveTarget[] = [];
  @State targetFilter: number = 0; // 0-进行中 1-已完成
  @State showCreateTarget: boolean = false;
  @State newTargetName: string = "";
  @State newTargetAmount: string = "";
  @State newTargetAmountError: string = "";
  @State showAddAmount: Record<string, boolean> = {};
  @State addAmountInput: Record<string, string> = {};
  @State addAmountErrors: Record<string, string> = {};
  @State isLoading: boolean = false;

  async aboutToAppear(): Promise<void> {
    this.isLoading = true;
    await Promise.all([this.loadBudget(), this.loadTargets(), this.loadCategories()]);
    this.isLoading = false;
  }

  async loadBudget(): Promise<void> {
    const res = await BudgetApi.getCurrent();
    if (res.code === 200 && res.data) {
      this.budget = res.data;
      // 构建分类预算渲染行
      this.buildCategoryBudgetRows(res.data);
      // 检查预算预警
      this.checkBudgetAlerts(res.data);
    }
  }

  /** 构建分类预算行数据（在类方法中可使用 this.budget 访问） */
  buildCategoryBudgetRows(data: BudgetData): void {
    const rows: CategoryBudgetRowData[] = [];
    if (data.categoryBudgets) {
      const keys: string[] = Object.keys(data.categoryBudgets);
      for (const key of keys) {
        const catId: number = parseInt(key);
        const budgetAmt: number = data.categoryBudgets[key] || 0;
        const spentAmt: number =
          data.categorySpent && data.categorySpent[key] ? data.categorySpent[key] : 0;
        let alertLevel: string = "";
        if (data.alertCategories) {
          for (const a of data.alertCategories) {
            if (a.categoryId === catId) {
              alertLevel = a.alertLevel;
              break;
            }
          }
        }
        let catName: string = `分类${catId}`;
        for (const c of this.categories) {
          if (c.id === catId) {
            catName = c.name;
            break;
          }
        }
        const row: CategoryBudgetRowData = {
          categoryId: catId,
          categoryName: catName,
          budgetAmount: budgetAmt,
          spentAmount: spentAmt,
          alertLevel: alertLevel,
        };
        rows.push(row);
      }
    }
    this.categoryBudgetRows = rows;
  }

  async loadCategories(): Promise<void> {
    const res = await CategoryApi.getList("expense");
    if (res.code === 200 && res.data) {
      this.categories = res.data;
    }
  }

  async loadTargets(): Promise<void> {
    const res = await SaveTargetApi.getList(this.targetFilter === 0 ? 0 : 1);
    if (res.code === 200 && res.data) {
      this.targets = res.data;
    }
  }

  /** 预算预警检查 - SRS F-M-004-04 */
  checkBudgetAlerts(data: BudgetData): void {
    if (data.alertCategories && data.alertCategories.length > 0) {
      for (const alert of data.alertCategories) {
        if (alert.alertLevel === "OVER") {
          ToastUtil.showWarning(`${alert.categoryName}预算已超出！`);
        } else if (alert.alertLevel === "WARNING") {
          ToastUtil.showWarning(
            `${alert.categoryName}预算即将超出（已达${((alert.spentAmount / alert.budgetAmount) * 100).toFixed(0)}%）`,
          );
        }
      }
    }
  }

  async onSaveBudget(): Promise<void> {
    if (!this.validateAllBudgetInputs()) {
      return;
    }

    const total = parseFloat(this.budgetTotalInput);
    if (!total || total <= 0) {
      ToastUtil.showError("请输入有效预算金额");
      return;
    }

    const catBudgets: Record<string, number> = {};
    for (const key of Object.keys(this.categoryBudgetInputs)) {
      const val = parseFloat(this.categoryBudgetInputs[key]);
      if (val && val > 0) catBudgets[key] = val;
    }

    const data: BudgetCreateRequest = {
      yearMonth: DateUtil.getCurrentYearMonth(),
      totalBudget: total,
      categoryBudgets: Object.keys(catBudgets).length > 0 ? catBudgets : undefined,
    };

    const res = await BudgetApi.createOrUpdate(data);
    if (res.code === 200 && res.data) {
      this.budget = res.data;
      this.buildCategoryBudgetRows(res.data);
      this.showBudgetSetting = false;
      ToastUtil.showSuccess("预算设置成功");
      this.checkBudgetAlerts(res.data);
    } else {
      ToastUtil.showError(res.message || "预算设置失败");
    }
  }

  validateAllBudgetInputs(): boolean {
    if (this.budgetTotalError !== "") {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return false;
    }
    for (const key of Object.keys(this.categoryBudgetErrors)) {
      if (this.categoryBudgetErrors[key] !== "") {
        AlertDialog.show({
          title: "提示",
          message: "您设置的金额非法，请重新设置！",
          confirm: { value: "确定", action: () => {} },
        });
        return false;
      }
    }
    if (
      this.budgetTotalInput &&
      this.budgetTotalInput !== "" &&
      (ValidationUtil.hasNonNumericChar(this.budgetTotalInput) ||
        ValidationUtil.validateAmount(this.budgetTotalInput) !== "")
    ) {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return false;
    }
    for (const key of Object.keys(this.categoryBudgetInputs)) {
      const val = this.categoryBudgetInputs[key];
      if (
        val &&
        val !== "" &&
        (ValidationUtil.hasNonNumericChar(val) || ValidationUtil.validateAmount(val) !== "")
      ) {
        AlertDialog.show({
          title: "提示",
          message: "您设置的金额非法，请重新设置！",
          confirm: { value: "确定", action: () => {} },
        });
        return false;
      }
    }
    return true;
  }

  clearBudgetErrors(): void {
    this.budgetTotalError = "";
    this.categoryBudgetErrors = {};
  }

  async onCreateTarget(): Promise<void> {
    if (this.newTargetAmountError !== "") {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
    if (
      this.newTargetAmount &&
      this.newTargetAmount !== "" &&
      (ValidationUtil.hasNonNumericChar(this.newTargetAmount) ||
        ValidationUtil.validateAmount(this.newTargetAmount) !== "")
    ) {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
    const amount = parseFloat(this.newTargetAmount);
    if (!this.newTargetName.trim()) {
      ToastUtil.showError("请输入目标名称");
      return;
    }
    if (!amount || amount <= 0) {
      ToastUtil.showError("请输入有效金额");
      return;
    }

    const res = await SaveTargetApi.create({
      name: this.newTargetName.trim(),
      targetAmount: amount,
    });

    if (res.code === 200) {
      ToastUtil.showSuccess("目标创建成功");
      this.showCreateTarget = false;
      this.newTargetName = "";
      this.newTargetAmount = "";
      this.newTargetAmountError = "";
      await this.loadTargets();
    } else {
      ToastUtil.showError(res.message || "创建失败");
    }
  }

  async onAddAmount(targetId: number, amount: number): Promise<void> {
    const inputVal = this.addAmountInput[targetId.toString()] || "";
    const inputError = this.addAmountErrors[targetId.toString()] || "";
    if (inputError !== "") {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
    if (
      inputVal &&
      inputVal !== "" &&
      (ValidationUtil.hasNonNumericChar(inputVal) || ValidationUtil.validateAmount(inputVal) !== "")
    ) {
      AlertDialog.show({
        title: "提示",
        message: "您设置的金额非法，请重新设置！",
        confirm: { value: "确定", action: () => {} },
      });
      return;
    }
    if (!amount || amount <= 0) {
      ToastUtil.showError("请输入有效金额");
      return;
    }
    const res = await SaveTargetApi.update(targetId, { addAmount: amount });
    if (res.code === 200) {
      ToastUtil.showSuccess("存款成功！");
      this.showAddAmount[targetId.toString()] = false;
      this.addAmountInput[targetId.toString()] = "";
      this.addAmountErrors[targetId.toString()] = "";
      await this.loadTargets();
    } else {
      ToastUtil.showError(res.message || "操作失败");
    }
  }

  async onDeleteTarget(id: number): Promise<void> {
    const res = await SaveTargetApi.delete(id);
    if (res.code === 200) {
      ToastUtil.showSuccess("已删除");
      this.loadTargets();
    } else {
      ToastUtil.showError(res.message || "删除失败");
    }
  }

  build() {
    Column() {
      this.buildTopBar()
      if (this.isLoading) {
        Column() {
          LoadingProgress()
            .width(40)
            .height(40)
            .color($r("app.color.primary"))
        }
        .width("100%")
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center);
      } else {
        Scroll() {
          Column() {
            this.buildBudgetSection()
            this.buildTargetSection()
          }
          .padding({ left: 16, right: 16, bottom: 30 })
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off);
      }
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
      Text("预算与目标")
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
  buildBudgetSection() {
    Column() {
      // 标题行
      Row() {
        Text("💵 月度预算")
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
        Blank()
        Text(this.showBudgetSetting ? "取消设置" : this.budget ? "修改预算" : "设置预算")
          .fontSize(13)
          .fontColor($r("app.color.primary"))
          .onClick(() => {
            if (!this.showBudgetSetting) {
              if (this.budget) {
                this.budgetTotalInput = this.budget.totalBudget.toString();
                const catInputs: Record<string, string> = {};
                if (this.budget.categoryBudgets) {
                  for (const key of Object.keys(this.budget.categoryBudgets)) {
                    catInputs[key] = this.budget.categoryBudgets[key].toString();
                  }
                }
                this.categoryBudgetInputs = catInputs;
              }
            } else {
              this.clearBudgetErrors();
            }
            this.showBudgetSetting = !this.showBudgetSetting;
          })
      }
      .width("100%")
      .margin({ bottom: 12 })

      if (this.budget && !this.showBudgetSetting) {
        // 预算概览
        Row() {
          Column() {
            Text("总预算")
              .fontSize(11)
              .fontColor($r("app.color.text_hint"))
            Text(DateUtil.formatMoney(this.budget.totalBudget))
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor("#4CAF50")
          }
          .flexGrow(1)
          .alignItems(HorizontalAlign.Center)
          Column() {
            Text("已消费")
              .fontSize(11)
              .fontColor($r("app.color.text_hint"))
            Text(DateUtil.formatMoney(this.budget.totalSpent))
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor("#FF9800")
          }
          .flexGrow(1)
          .alignItems(HorizontalAlign.Center)
          Column() {
            Text("剩余")
              .fontSize(11)
              .fontColor($r("app.color.text_hint"))
            Text(DateUtil.formatMoney(this.budget.remaining))
              .fontSize(16)
              .fontWeight(FontWeight.Bold)
              .fontColor("#4A90D9")
          }
          .flexGrow(1)
          .alignItems(HorizontalAlign.Center)
        }
        .width("100%")
        .padding(16)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(12)
        .margin({ bottom: 12 });

        // 总进度条
        Text(`已使用 ${this.budget.usagePercent.toFixed(1)}%`)
          .fontSize(11)
          .fontColor($r("app.color.text_hint"))
          .margin({ bottom: 4 })
          .width("100%");
        Progress({ value: this.budget.usagePercent, total: 100, type: ProgressType.Linear })
          .width("100%")
          .height(10)
          .color(
            this.budget.usagePercent >= 100
              ? "#FF4444"
              : this.budget.usagePercent >= 80
                ? "#FF9800"
                : "#4CAF50",
          );

        // 分类子预算进度
        if (this.budget.categoryBudgets) {
          Text("分类预算明细")
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor($r("app.color.text_primary"))
            .width("100%")
            .margin({ top: 16, bottom: 8 });
          ForEach(this.categoryBudgetRows, (item: CategoryBudgetRowData) => {
            BudgetProgress({
              categoryName: item.categoryName,
              spentAmount: item.spentAmount,
              budgetAmount: item.budgetAmount,
              alertLevel: item.alertLevel,
            });
          });
        }
      } else if (this.showBudgetSetting) {
        // 预算设置表单
        Column() {
          Text("月度总预算")
            .fontSize(12)
            .fontColor($r("app.color.text_hint"))
            .width("100%")
            .margin({ bottom: 4 })
          TextInput({ text: this.budgetTotalInput })
            .type(InputType.Normal)
            .fontSize(16)
            .height(44)
            .backgroundColor($r("app.color.bg_card"))
            .borderRadius(8)
            .width("100%")
            .padding({ left: 12, right: 12 })
            .onChange((v: string) => {
              const hasNonNumeric = ValidationUtil.hasNonNumericChar(v);
              const filtered = ValidationUtil.filterAmountInput(v);
              this.budgetTotalInput = filtered;
              if (hasNonNumeric) {
                this.budgetTotalError = "⚠️警告：请输入数字";
              } else {
                this.budgetTotalError = ValidationUtil.validateAmount(filtered);
              }
            })

          if (this.budgetTotalError) {
            Text(this.budgetTotalError)
              .fontSize(12)
              .fontColor("#FF9800")
              .width("100%")
              .margin({ top: 2, left: 4 });
          }
          Text("各分类子预算（可选）")
            .fontSize(12)
            .fontColor($r("app.color.text_hint"))
            .width("100%")
            .margin({ top: 12, bottom: 6 })
          ForEach(
            this.categories.filter((c) => c.type === "expense"),
            (cat: Category) => {
              Column() {
                Row() {
                  Text(cat.name)
                    .fontSize(13)
                    .fontColor($r("app.color.text_secondary"))
                    .width(60)
                  TextInput({ text: this.categoryBudgetInputs[cat.id.toString()] || "" })
                    .type(InputType.Normal)
                    .fontSize(13)
                    .height(36)
                    .backgroundColor($r("app.color.bg_card"))
                    .borderRadius(6)
                    .layoutWeight(1)
                    .padding({ left: 8, right: 8 })
                    .onChange((v: string) => {
                      const hasNonNumeric = ValidationUtil.hasNonNumericChar(v);
                      const filtered = ValidationUtil.filterAmountInput(v);
                      this.categoryBudgetInputs[cat.id.toString()] = filtered;
                      if (hasNonNumeric) {
                        this.categoryBudgetErrors[cat.id.toString()] = "⚠️警告：请输入数字";
                      } else {
                        this.categoryBudgetErrors[cat.id.toString()] =
                          ValidationUtil.validateAmount(filtered);
                      }
                    })
                }
                .width("100%")
                .margin({ top: 4 })

                if (this.categoryBudgetErrors[cat.id.toString()]) {
                  Text(this.categoryBudgetErrors[cat.id.toString()])
                    .fontSize(11)
                    .fontColor("#FF9800")
                    .width("100%")
                    .margin({ top: 2, left: 64 });
                }
              }.width("100%");
            },
          )
          Button("保存预算")
            .fontSize(14)
            .backgroundColor($r("app.color.primary"))
            .fontColor(Color.White)
            .borderRadius(20)
            .height(40)
            .width("100%")
            .margin({ top: 16 })
            .onClick(() => {
              this.onSaveBudget();
            })
        }
        .width("100%")
        .padding(16)
        .backgroundColor($r("app.color.bg_card"))
        .borderRadius(12);
      } else {
        EmptyView({ message: "暂无预算，点击设置" });
      }
    }
    .width("100%")
    .padding(16)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildTargetSection() {
    Column() {
      Row() {
        Text("🎯 存钱目标")
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
        Blank()
        Text("+ 新建")
          .fontSize(13)
          .fontColor($r("app.color.primary"))
          .onClick(() => {
            this.showCreateTarget = !this.showCreateTarget;
          })
      }
      .width("100%")
      .margin({ bottom: 12 })

      // 状态切换
      Row() {
        Button("进行中")
          .fontSize(12)
          .height(28)
          .borderRadius(14)
          .backgroundColor(
            this.targetFilter === 0 ? $r("app.color.primary") : $r("app.color.bg_page"),
          )
          .fontColor(this.targetFilter === 0 ? Color.White : $r("app.color.text_secondary"))
          .onClick(() => {
            this.targetFilter = 0;
            this.loadTargets();
          })
        Button("已完成")
          .fontSize(12)
          .height(28)
          .borderRadius(14)
          .backgroundColor(
            this.targetFilter === 1 ? $r("app.color.success") : $r("app.color.bg_page"),
          )
          .fontColor(this.targetFilter === 1 ? Color.White : $r("app.color.text_secondary"))
          .margin({ left: 8 })
          .onClick(() => {
            this.targetFilter = 1;
            this.loadTargets();
          })
      }
      .width("100%")
      .margin({ bottom: 10 })

      // 新建目标表单
      if (this.showCreateTarget) {
        Column() {
          TextInput({ placeholder: "目标名称", text: this.newTargetName })
            .fontSize(14)
            .height(40)
            .backgroundColor($r("app.color.bg_page"))
            .borderRadius(8)
            .width("100%")
            .padding({ left: 10, right: 10 })
            .onChange((v: string) => {
              this.newTargetName = v;
            })
          TextInput({ placeholder: "目标金额", text: this.newTargetAmount })
            .type(InputType.Normal)
            .fontSize(14)
            .height(40)
            .backgroundColor($r("app.color.bg_page"))
            .borderRadius(8)
            .width("100%")
            .padding({ left: 10, right: 10 })
            .margin({ top: 8 })
            .onChange((v: string) => {
              const hasNonNumeric = ValidationUtil.hasNonNumericChar(v);
              const filtered = ValidationUtil.filterAmountInput(v);
              this.newTargetAmount = filtered;
              if (hasNonNumeric) {
                this.newTargetAmountError = "⚠️警告：请输入数字";
              } else {
                this.newTargetAmountError = ValidationUtil.validateAmount(filtered);
              }
            })

          if (this.newTargetAmountError) {
            Text(this.newTargetAmountError)
              .fontSize(12)
              .fontColor("#FF9800")
              .width("100%")
              .margin({ top: 2, left: 4 });
          }
          Button("创建目标")
            .fontSize(13)
            .backgroundColor($r("app.color.primary"))
            .fontColor(Color.White)
            .borderRadius(18)
            .height(36)
            .width("100%")
            .margin({ top: 10 })
            .onClick(() => {
              this.onCreateTarget();
            })
        }
        .width("100%")
        .padding(12)
        .backgroundColor($r("app.color.bg_page"))
        .borderRadius(8)
        .margin({ bottom: 10 });
      }

      // 目标列表
      if (this.targets.length === 0) {
        EmptyView({ message: this.targetFilter === 0 ? "暂无进行中的目标" : "暂无已完成的目标" });
      } else {
        ForEach(this.targets, (target: SaveTarget) => {
          Column() {
            Row() {
              Text(target.name)
                .fontSize(15)
                .fontWeight(FontWeight.Medium)
                .fontColor($r("app.color.text_primary"))
                .layoutWeight(1)
              if (target.status === 1) {
                Text("✅ 已完成").fontSize(11).fontColor($r("app.color.success"));
              }
              Text("🗑")
                .fontSize(14)
                .margin({ left: 8 })
                .onClick(() => {
                  this.onDeleteTarget(target.id);
                })
            }.width("100%")
            Text(
              `目标：${DateUtil.formatMoney(target.targetAmount)}  |  已存：${DateUtil.formatMoney(target.savedAmount)}`,
            )
              .fontSize(12)
              .fontColor($r("app.color.text_hint"))
              .margin({ top: 4 })
              .width("100%")
            Progress({ value: target.progressPercent, total: 100, type: ProgressType.Linear })
              .width("100%")
              .height(8)
              .margin({ top: 6 })
              .color(
                target.status === 1
                  ? "#4CAF50"
                  : target.progressPercent >= 80
                    ? "#FF9800"
                    : "#4A90D9",
              )

            if (target.status === 0) {
              Row() {
                Button("💰 存入")
                  .fontSize(11)
                  .height(26)
                  .backgroundColor($r("app.color.bg_card"))
                  .fontColor($r("app.color.success"))
                  .borderRadius(13)
                  .onClick(() => {
                    this.showAddAmount[target.id.toString()] =
                      !this.showAddAmount[target.id.toString()];
                  })
              }
              .width("100%")
              .margin({ top: 6 });

              if (this.showAddAmount[target.id.toString()]) {
                Column() {
                  Row() {
                    TextInput({
                      placeholder: "金额",
                      text: this.addAmountInput[target.id.toString()] || "",
                    })
                      .type(InputType.Normal)
                      .fontSize(13)
                      .height(32)
                      .backgroundColor($r("app.color.bg_page"))
                      .borderRadius(16)
                      .layoutWeight(1)
                      .padding({ left: 10, right: 10 })
                      .onChange((v: string) => {
                        const hasNonNumeric = ValidationUtil.hasNonNumericChar(v);
                        const filtered = ValidationUtil.filterAmountInput(v);
                        this.addAmountInput[target.id.toString()] = filtered;
                        if (hasNonNumeric) {
                          this.addAmountErrors[target.id.toString()] = "⚠️警告：请输入数字";
                        } else {
                          this.addAmountErrors[target.id.toString()] =
                            ValidationUtil.validateAmount(filtered);
                        }
                      })
                    Button("确认")
                      .fontSize(11)
                      .height(28)
                      .backgroundColor($r("app.color.success"))
                      .fontColor(Color.White)
                      .borderRadius(14)
                      .margin({ left: 8 })
                      .onClick(() => {
                        const amt = parseFloat(this.addAmountInput[target.id.toString()]);
                        this.onAddAmount(target.id, amt);
                      })
                  }
                  .width("100%")
                  .margin({ top: 4 })

                  if (this.addAmountErrors[target.id.toString()]) {
                    Text(this.addAmountErrors[target.id.toString()])
                      .fontSize(11)
                      .fontColor("#FF9800")
                      .width("100%")
                      .margin({ top: 2, left: 4 });
                  }
                }
                .width("100%")
                .margin({ top: 6 });
              }
            }
          }
          .width("100%")
          .padding(12)
          .backgroundColor($r("app.color.bg_card"))
          .borderRadius(8)
          .margin({ top: 8 });
        });
      }
    }
    .width("100%")
    .padding(16)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(12)
    .margin({ top: 16 });
  }
}
