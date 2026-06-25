/**
 * 月度统计图表页
 * 功能：收支概览、支出分类占比饼图、每日收支趋势柱状图、月份切换
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { StatisticsApi } from "../../network/api/statisticsApi";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { MonthlyStatistics, CategoryBreakdown, DailyBreakdown } from "../../model/StatisticsModel";
import { PieChart, PieChartData } from "../../components/chart/PieChart";
import { BarChart, BarChartData } from "../../components/chart/BarChart";
import { EmptyView } from "../../components/common/EmptyView";

@Entry
@Component
struct StatisticsPage {
  // P2-2 修复：月份上下界保护
  private static readonly MIN_YEARMONTH: string = "2020-01";
  private static readonly MAX_YEARMONTH: string = "2099-12";

  @State statistics: MonthlyStatistics | null = null;
  @State isLoading: boolean = false;
  @State yearMonth: string = DateUtil.getCurrentYearMonth();

  async aboutToAppear(): Promise<void> {
    await this.loadStatistics();
  }

  async loadStatistics(): Promise<void> {
    this.isLoading = true;
    try {
      const res = await StatisticsApi.getMonthly(this.yearMonth);
      if (res.code === 200) {
        this.statistics = res.data;
      } else if (res.code === -1) {
        ToastUtil.showWarning("网络不可达，无法加载统计");
      }
    } catch (err) {
      console.error("加载统计失败:", JSON.stringify(err));
    }
    this.isLoading = false;
  }

  onPrevMonth(): void {
    // P2-2 修复：月份下界保护
    const next: string = DateUtil.addMonths(this.yearMonth, -1);
    if (next < StatisticsPage.MIN_YEARMONTH) {
      ToastUtil.showInfo("已是最早可查询月份（" + StatisticsPage.MIN_YEARMONTH + "）");
      return;
    }
    this.yearMonth = next;
    this.loadStatistics();
  }

  onNextMonth(): void {
    // P2-2 修复：月份上界保护
    const next: string = DateUtil.addMonths(this.yearMonth, 1);
    if (next > StatisticsPage.MAX_YEARMONTH) {
      ToastUtil.showInfo("已是最新可查询月份（" + StatisticsPage.MAX_YEARMONTH + "）");
      return;
    }
    this.yearMonth = next;
    this.loadStatistics();
  }

  /** 构建饼图数据 */
  getPieData(): PieChartData[] {
    if (!this.statistics?.categoryBreakdown) return [];
    const colors = [
      "#4A90D9",
      "#FF6B6B",
      "#4CAF50",
      "#FF9800",
      "#9C27B0",
      "#00BCD4",
      "#FFEB3B",
      "#795548",
      "#607D8B",
      "#E91E63",
    ];
    return this.statistics.categoryBreakdown
      .filter((c) => c.totalAmount > 0)
      .map((c, i): PieChartData => {
        const pieItem: PieChartData = {
          name: c.categoryName,
          value: c.totalAmount,
          color: colors[i % colors.length],
        };
        return pieItem;
      });
  }

  /** 构建柱状图数据 */
  getBarData(): BarChartData[] {
    if (!this.statistics?.dailyBreakdown) return [];
    return this.statistics.dailyBreakdown.map((d): BarChartData => {
      const barItem: BarChartData = {
        label: d.date?.substring(8) || "",
        income: d.income,
        expense: d.expense,
      };
      return barItem;
    });
  }

  build() {
    Column() {
      // 顶部导航
      this.buildTopBar()

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
      } else if (!this.statistics || this.statistics.billCount === 0) {
        EmptyView({ message: "本月暂无账单数据" });
      } else {
        Scroll() {
          Column() {
            // 本月概况
            this.buildOverview()

            // 支出分类饼图
            this.buildPieSection()

            // 每日趋势柱状图
            this.buildBarSection()

            // 分类明细列表
            this.buildCategoryList()
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
      Text("月度统计")
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
  buildOverview() {
    Column() {
      // 月份切换
      Row() {
        Image($r("sys.media.ohos_ic_public_arrow_left"))
          .width(18)
          .height(18)
          .onClick(() => {
            this.onPrevMonth();
          })
        Text(DateUtil.formatYearMonthCN(this.yearMonth))
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor($r("app.color.text_primary"))
          .margin({ left: 16, right: 16 })
        Image($r("sys.media.ohos_ic_public_arrow_right"))
          .width(18)
          .height(18)
          .onClick(() => {
            this.onNextMonth();
          })
      }
      .justifyContent(FlexAlign.Center)
      .width("100%")
      .margin({ bottom: 16 })

      // 三个数据卡片
      Row() {
        this.buildCard("总收入", this.statistics!.totalIncome, "#4CAF50")
        this.buildCard("总支出", this.statistics!.totalExpense, "#FF6B6B")
        this.buildCard("月度结余", this.statistics!.balance, "#4A90D9")
      }.width("100%")
    }
    .width("100%")
    .margin({ top: 16 });
  }

  @Builder
  buildCard(title: string, amount: number, color: string) {
    Column() {
      Text(title)
        .fontSize(11)
        .fontColor($r("app.color.text_hint"))
        .margin({ bottom: 6 })
      Text(DateUtil.formatMoney(amount))
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor(color)
    }
    .flexGrow(1)
    .height(70)
    .justifyContent(FlexAlign.Center)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(10)
    .margin({ left: 4, right: 4 });
  }

  @Builder
  buildPieSection() {
    Column() {
      Text("📊 支出分类占比")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ bottom: 8 })
      PieChart({ data: this.getPieData() })
    }
    .width("100%")
    .padding(16)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildBarSection() {
    Column() {
      Text("📈 每日收支趋势")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ bottom: 8 })
      BarChart({ data: this.getBarData() })
    }
    .width("100%")
    .padding(16)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildCategoryList() {
    Column() {
      Text("📋 分类明细")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .width("100%")
        .margin({ bottom: 8 })
      ForEach(this.statistics!.categoryBreakdown || [], (item: CategoryBreakdown) => {
        Row() {
          Text(item.categoryName)
            .fontSize(14)
            .fontColor($r("app.color.text_primary"))
            .layoutWeight(1)
          Text(DateUtil.formatMoney(item.totalAmount))
            .fontSize(14)
            .fontColor($r("app.color.text_secondary"))
            .margin({ right: 8 })
          Text(`${item.percentage.toFixed(1)}%`)
            .fontSize(12)
            .fontColor($r("app.color.text_hint"))
        }
        .width("100%")
        .padding({ top: 12, bottom: 12 })
        .border({ width: { bottom: 0.5 }, color: $r("app.color.bg_page") });
      })
    }
    .width("100%")
    .padding(16)
    .backgroundColor($r("app.color.bg_card"))
    .borderRadius(12)
    .margin({ top: 16 });
  }
}
