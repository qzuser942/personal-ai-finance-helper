/**
 * AI智能理财分析页
 * 功能：一键月度财务诊断、AI分析结果展示、分析历史查看
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { AiApi } from "../../network/api/aiApi";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import {
  AiAnalysisResult,
  AiHistoryItem,
  RedundantItem,
  BadHabit,
  SavingPlan,
} from "../../model/AiModel";
import { EmptyView } from "../../components/common/EmptyView";

@Entry
@Component
struct AiAnalysisPage {
  @State analysisResult: AiAnalysisResult | null = null;
  @State historyList: AiHistoryItem[] = [];
  @State isLoading: boolean = false;
  @State yearMonth: string = DateUtil.getCurrentYearMonth();
  @State showHistory: boolean = false;
  @State historyPage: number = 1;

  async aboutToAppear(): Promise<void> {
    await this.loadHistory();
  }

  async onStartAnalysis(): Promise<void> {
    this.isLoading = true;
    this.analysisResult = null;
    ToastUtil.showInfo("正在分析您的消费数据，预计需要5-30秒...");

    try {
      const res = await AiApi.analyze({ yearMonth: this.yearMonth });
      if (res.code === 200 && res.data) {
        this.analysisResult = res.data;
        ToastUtil.showSuccess(`AI分析完成，耗时${(res.data.processingTimeMs / 1000).toFixed(1)}秒`);
        await this.loadHistory();
      } else if (res.code === 40001) {
        ToastUtil.showError("该月份无账单数据，请先记账");
      } else if (res.code === -1) {
        ToastUtil.showError("网络不可达，AI分析需要联网");
      } else {
        ToastUtil.showError(res.message || "AI分析失败，请稍后重试");
      }
    } catch (err) {
      ToastUtil.showError("AI服务异常，请重试");
    } finally {
      this.isLoading = false;
    }
  }

  async loadHistory(): Promise<void> {
    const res = await AiApi.getHistory(1, 10);
    if (res.code === 200 && res.data) {
      this.historyList = res.data.records;
    }
  }

  async onLoadHistoryDetail(id: number): Promise<void> {
    this.isLoading = true;
    const res = await AiApi.getHistoryDetail(id);
    if (res.code === 200 && res.data) {
      this.analysisResult = res.data;
      this.showHistory = false;
    }
    this.isLoading = false;
  }

  build() {
    Column() {
      this.buildTopBar()
      Scroll() {
        Column() {
          this.buildAnalyzeControl()
          if (this.isLoading) {
            this.buildLoadingState();
          } else if (this.analysisResult) {
            this.buildAnalysisResult();
          } else if (this.showHistory) {
            this.buildHistoryList();
          } else {
            EmptyView({ message: "点击上方按钮开始AI理财分析" });
          }
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
      Text("AI理财分析")
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .margin({ left: 8 })
      Blank()
      Text(this.showHistory ? "新分析" : "历史")
        .fontSize(13)
        .fontColor($r("app.color.primary"))
        .onClick(() => {
          this.showHistory = !this.showHistory;
          this.analysisResult = null;
        })
    }
    .width("100%")
    .padding({ left: 16, right: 16, top: 40, bottom: 10 })
    .backgroundColor($r("app.color.bg_card"));
  }

  @Builder
  buildAnalyzeControl() {
    Column() {
      // 月份选择
      Row() {
        Image($r("sys.media.ohos_ic_public_arrow_left"))
          .width(16)
          .height(16)
          .onClick(() => {
            this.yearMonth = DateUtil.addMonths(this.yearMonth, -1);
          })
        Text(DateUtil.formatYearMonthCN(this.yearMonth))
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor("#333333")
          .margin({ left: 12, right: 12 })
        Image($r("sys.media.ohos_ic_public_arrow_right"))
          .width(16)
          .height(16)
          .onClick(() => {
            this.yearMonth = DateUtil.addMonths(this.yearMonth, 1);
          })
      }
      .justifyContent(FlexAlign.Center)
      .width("100%")
      .margin({ bottom: 16 })
      Button("🚀 开始AI分析")
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .backgroundColor("#9C27B0")
        .fontColor("#FFFFFF")
        .borderRadius(24)
        .height(48)
        .width("80%")
        .enabled(!this.isLoading)
        .onClick(() => {
          this.onStartAnalysis();
        })
    }
    .width("100%")
    .padding(24)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildLoadingState() {
    Column() {
      LoadingProgress()
        .width(50)
        .height(50)
        .color("#9C27B0")
      Text("正在分析您的消费数据...")
        .fontSize(15)
        .fontColor("#666666")
        .margin({ top: 16 })
      Text("AI正在深度分析您的消费习惯，请耐心等待")
        .fontSize(12)
        .fontColor("#BBBBBB")
        .margin({ top: 8 })
      Text("预计耗时 5-30 秒")
        .fontSize(11)
        .fontColor("#CCCCCC")
        .margin({ top: 4 })
    }
    .width("100%")
    .padding(40)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 })
    .alignItems(HorizontalAlign.Center);
  }

  @Builder
  buildAnalysisResult() {
    Column() {
      // 月度复盘
      if (this.analysisResult!.monthlyReview) {
        this.buildCardMonthlyReview();
      }

      // 冗余消费
      if (this.analysisResult!.redundantItems && this.analysisResult!.redundantItems.length > 0) {
        this.buildCardRedundant();
      }

      // 不良习惯
      if (this.analysisResult!.badHabits && this.analysisResult!.badHabits.length > 0) {
        this.buildCardBadHabits();
      }

      // 省钱方案
      if (this.analysisResult!.savingPlans && this.analysisResult!.savingPlans.length > 0) {
        this.buildCardSavingPlans();
      }

      // 处理时间
      Text(`⏱ AI处理耗时：${(this.analysisResult!.processingTimeMs / 1000).toFixed(1)}秒`)
        .fontSize(11)
        .fontColor("#CCCCCC")
        .width("100%")
        .margin({ top: 16 })
    };
  }

  @Builder
  buildHistoryList() {
    Column() {
      Text("📋 分析历史")
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor("#333333")
        .width("100%")
        .margin({ bottom: 12 })

      if (this.historyList.length === 0) {
        EmptyView({ message: "暂无分析记录" });
      } else {
        ForEach(this.historyList, (item: AiHistoryItem) => {
          Row() {
            Column() {
              Text(DateUtil.formatYearMonthCN(item.yearMonth))
                .fontSize(14)
                .fontWeight(FontWeight.Medium)
                .fontColor("#333333")
              Text(item.resultPreview)
                .fontSize(12)
                .fontColor("#999999")
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
                .margin({ top: 2 })
            }
            .layoutWeight(1)
            .alignItems(HorizontalAlign.Start)
            Image($r("sys.media.ohos_ic_public_arrow_right"))
              .width(16)
              .height(16)
              .fillColor("#CCCCCC")
          }
          .width("100%")
          .padding(14)
          .backgroundColor("#FFFFFF")
          .borderRadius(8)
          .margin({ top: 8 })
          .onClick(() => {
            this.onLoadHistoryDetail(item.id);
          });
        });
      }
    }
    .width("100%")
    .margin({ top: 16 });
  }

  @Builder
  buildCard(title: string) {
    Column() {
      Text(title)
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor("#333333")
        .width("100%")
        .margin({ bottom: 10 })
    }
    .width("100%")
    .padding(16)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildCardMonthlyReview() {
    Column() {
      Text("📝 月度财务复盘")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor("#333333")
        .width("100%")
        .margin({ bottom: 10 })
      Text(this.analysisResult!.monthlyReview)
        .fontSize(14)
        .fontColor("#555555")
        .lineHeight(22)
        .width("100%")
    }
    .width("100%")
    .padding(16)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildCardRedundant() {
    Column() {
      Text("⚠️ 冗余消费项")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor("#333333")
        .width("100%")
        .margin({ bottom: 10 })
      ForEach(this.analysisResult!.redundantItems, (item: RedundantItem) => {
        Column() {
          Row() {
            Text(item.name)
              .fontSize(14)
              .fontWeight(FontWeight.Medium)
              .fontColor("#FF6B6B")
              .layoutWeight(1)
            Text(DateUtil.formatMoney(item.amount))
              .fontSize(14)
              .fontColor("#FF6B6B")
          }.width("100%")
          Text(`原因：${item.reason}`)
            .fontSize(12)
            .fontColor("#999999")
            .margin({ top: 4 })
            .width("100%")
          Text(`建议：${item.suggestion}`)
            .fontSize(12)
            .fontColor("#4CAF50")
            .margin({ top: 2 })
            .width("100%")
        }
        .width("100%")
        .padding(10)
        .backgroundColor("#FFF5F5")
        .borderRadius(8)
        .margin({ top: 6 });
      })
    }
    .width("100%")
    .padding(16)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildCardBadHabits() {
    Column() {
      Text("🔍 不良消费习惯")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor("#333333")
        .width("100%")
        .margin({ bottom: 10 })
      ForEach(this.analysisResult!.badHabits, (item: BadHabit) => {
        Column() {
          Text(item.habit)
            .fontSize(14)
            .fontWeight(FontWeight.Medium)
            .fontColor("#FF9800")
            .width("100%")
          Text(item.description)
            .fontSize(12)
            .fontColor("#999999")
            .margin({ top: 4 })
            .width("100%")
          Text(`影响：${item.impact}`)
            .fontSize(12)
            .fontColor("#FF6B6B")
            .margin({ top: 2 })
            .width("100%")
        }
        .width("100%")
        .padding(10)
        .backgroundColor("#FFF8E1")
        .borderRadius(8)
        .margin({ top: 6 });
      })
    }
    .width("100%")
    .padding(16)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 });
  }

  @Builder
  buildCardSavingPlans() {
    Column() {
      Text("💡 个性化省钱方案")
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor("#333333")
        .width("100%")
        .margin({ bottom: 10 })
      ForEach(this.analysisResult!.savingPlans, (item: SavingPlan, index: number) => {
        Column() {
          Text(`${index + 1}. ${item.plan}`)
            .fontSize(14)
            .fontWeight(FontWeight.Medium)
            .fontColor("#4CAF50")
            .width("100%")
          Text(item.description)
            .fontSize(12)
            .fontColor("#666666")
            .margin({ top: 4 })
            .width("100%")
          Text(`预估节省：${item.estimatedSave}`)
            .fontSize(12)
            .fontColor("#4A90D9")
            .margin({ top: 2 })
            .width("100%")
        }
        .width("100%")
        .padding(10)
        .backgroundColor("#E8F5E9")
        .borderRadius(8)
        .margin({ top: 6 });
      })
    }
    .width("100%")
    .padding(16)
    .backgroundColor("#FFFFFF")
    .borderRadius(12)
    .margin({ top: 16 });
  }
}
