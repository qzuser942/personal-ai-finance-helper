/**
 * 账单流水页面
 * 功能：分页加载、下拉刷新、上拉加载更多、多维度筛选、Excel导出
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { BillApi, BillSearchParams } from "../../network/api/billApi";
import { CategoryApi } from "../../network/api/categoryApi";
import { AppState } from "../../store/AppState";
import { HttpRequest } from "../../network/request";
import { DateUtil } from "../../utils/date";
import { ToastUtil } from "../../utils/toast";
import { BillListItem } from "../../model/BillModel";
import { Category } from "../../model/CategoryModel";
import { BillItem } from "../../components/bill/BillItem";
import { EmptyView } from "../../components/common/EmptyView";
import { ConfirmDialog } from "../../components/common/ConfirmDialog";
import { CsvExportUtil } from "../../utils/CsvExportUtil";

@Entry
@Component
struct BillListPage {
  @State bills: BillListItem[] = [];
  @State currentPage: number = 1;
  @State totalPages: number = 1;
  @State isLoading: boolean = false;
  @State isRefreshing: boolean = false;
  @State hasMore: boolean = true;

  // 筛选条件
  @State filterType: string = ""; // '' | 'income' | 'expense'
  @State filterCategoryId: number = 0;
  @State filterYearMonth: string = DateUtil.getCurrentYearMonth();
  @State filterKeyword: string = "";
  @State categories: Category[] = [];
  @State showMonthPicker: boolean = false;
  @State showDeleteDialog: boolean = false;
  @State deleteTargetId: number = 0;

  async aboutToAppear(): Promise<void> {
    await this.loadCategories();
    await this.refreshData();
  }

  onPageShow(): void {
    this.refreshData();
  }

  async loadCategories(): Promise<void> {
    const res = await CategoryApi.getList();
    if (res.code === 200 && res.data) {
      this.categories = res.data;
    }
  }

  async refreshData(): Promise<void> {
    this.currentPage = 1;
    this.isRefreshing = true;
    await this.fetchBills();
    this.isRefreshing = false;
  }

  async loadMore(): Promise<void> {
    if (!this.hasMore || this.isLoading) return;
    this.currentPage++;
    this.isLoading = true;
    await this.fetchBills(true);
    this.isLoading = false;
  }

  async fetchBills(append: boolean = false): Promise<void> {
    const params: BillSearchParams = {
      page: this.currentPage,
      size: 20,
    };
    if (this.filterType) params.type = this.filterType;
    if (this.filterCategoryId > 0) params.categoryId = this.filterCategoryId;
    if (this.filterYearMonth) params.yearMonth = this.filterYearMonth;
    if (this.filterKeyword) params.keyword = this.filterKeyword;

    const res = await BillApi.search(params);
    if (res.code === 200 && res.data) {
      if (append) {
        this.bills = this.bills.concat(res.data.records);
      } else {
        this.bills = res.data.records;
      }
      this.totalPages = res.data.totalPages;
      this.hasMore = this.currentPage < this.totalPages;
    } else if (res.code === -1) {
      ToastUtil.showWarning("网络不可达，无法加载账单");
    }
  }

  onTypeFilter(type: string): void {
    this.filterType = this.filterType === type ? "" : type;
    this.refreshData();
  }

  onBillClick(bill: BillListItem): void {
    router.pushUrl({
      url: "pages/bill/BillDetailPage",
      params: { billId: bill.id },
    });
  }

  onDeleteConfirm(billId: number): void {
    this.deleteTargetId = billId;
    this.showDeleteDialog = true;
  }

  async onDeleteBill(): Promise<void> {
    if (!this.deleteTargetId) return;
    const res = await BillApi.delete(this.deleteTargetId);
    if (res.code === 200) {
      ToastUtil.showSuccess("已删除");
      this.refreshData();
    } else {
      ToastUtil.showError(res.message || "删除失败");
    }
    this.showDeleteDialog = false;
    this.deleteTargetId = 0;
  }

  async onExportExcel(): Promise<void> {
    if (!this.filterYearMonth) {
      ToastUtil.showError("请选择月份");
      return;
    }
    ToastUtil.showInfo("正在生成Excel文件...");
    try {
      const sep: string = "?";
      const filterTypePart: string = this.filterType
        ? `&type=${encodeURIComponent(this.filterType)}`
        : "";
      const downloadUrl: string = `${AppState.BASE_URL}/api/bill/export${sep}yearMonth=${encodeURIComponent(this.filterYearMonth)}${filterTypePart}`;
      const fileName: string = `bills_${this.filterYearMonth}_${Date.now()}.xlsx`;
      const savedPath: string = await HttpRequest.downloadToFile(downloadUrl, fileName);
      const displayName: string = savedPath.substring(savedPath.lastIndexOf("/") + 1);
      ToastUtil.showSuccess(`Excel已导出：${displayName}`);
    } catch (err) {
      console.error("Excel导出失败，尝试CSV导出:", JSON.stringify(err));
      try {
        const csvPath: string = await CsvExportUtil.exportToCsv(
          this.filterYearMonth,
          this.filterType || undefined,
        );
        const displayName: string = csvPath.substring(csvPath.lastIndexOf("/") + 1);
        ToastUtil.showSuccess(`已导出CSV：${displayName}`);
      } catch (csvErr) {
        console.error("CSV导出也失败:", JSON.stringify(csvErr));
        const msg: string = err instanceof Error ? err.message : String(err);
        ToastUtil.showError("导出失败：" + (msg || "网络异常"));
      }
    }
  }

  /** 月份切换 */
  onPrevMonth(): void {
    this.filterYearMonth = DateUtil.addMonths(this.filterYearMonth, -1);
    this.refreshData();
  }

  onNextMonth(): void {
    this.filterYearMonth = DateUtil.addMonths(this.filterYearMonth, 1);
    this.refreshData();
  }

  build() {
    Column() {
      // 顶部导航
      this.buildTopBar()

      // 筛选区域
      this.buildFilters()

      // 账单列表
      if (this.bills.length === 0) {
        EmptyView({ message: "暂无账单记录" });
      } else {
        List({ space: 8 }) {
          ForEach(this.bills, (bill: BillListItem, index: number) => {
            ListItem() {
              BillItem({
                bill: bill,
                categoryName: this.getCategoryName(bill.categoryId),
                iconName: "",
                onItemClick: () => {
                  this.onBillClick(bill);
                },
              })
            }.gesture(
              LongPressGesture({ duration: 600 }).onAction(() => {
                this.onDeleteConfirm(bill.id);
              }),
            );
          })

          // 底部加载更多
          ListItem() {
            Row() {
              if (this.isLoading) {
                LoadingProgress().width(20).height(20).color($r("app.color.primary"));
                Text("加载中...")
                  .fontSize(12)
                  .fontColor($r("app.color.text_hint"))
                  .margin({ left: 8 });
              } else if (this.hasMore) {
                Text("上滑加载更多").fontSize(12).fontColor($r("app.color.text_hint"));
              } else {
                Text("— 没有更多了 —").fontSize(12).fontColor($r("app.color.text_hint"));
              }
            }
            .width("100%")
            .height(50)
            .justifyContent(FlexAlign.Center)
          }
        }
        .layoutWeight(1)
        .width("100%")
        .padding({ left: 16, right: 16 })
        .onReachEnd(() => {
          this.loadMore();
        })
        .scrollBar(BarState.Off);
      }

      // 删除确认弹窗
      if (this.showDeleteDialog) {
        ConfirmDialog({
          title: "删除确认",
          message: "确定删除该账单记录吗？此操作不可恢复。",
          confirmText: "删除",
          cancelText: "取消",
          onConfirm: () => {
            this.onDeleteBill();
          },
        });
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
      Text("账单流水")
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .fontColor($r("app.color.text_primary"))
        .margin({ left: 8 })
      Blank()
      Button("📥 导出")
        .fontSize(12)
        .height(30)
        .backgroundColor($r("app.color.primary"))
        .fontColor(Color.White)
        .borderRadius(15)
        .padding({ left: 12, right: 12 })
        .onClick(() => {
          this.onExportExcel();
        })
    }
    .width("100%")
    .padding({ left: 16, right: 16, top: 40, bottom: 10 })
    .backgroundColor($r("app.color.bg_card"));
  }

  @Builder
  buildFilters() {
    Column() {
      // 月份切换器
      Row() {
        Image($r("sys.media.ohos_ic_public_arrow_left"))
          .width(18)
          .height(18)
          .onClick(() => {
            this.onPrevMonth();
          })
        Text(DateUtil.formatYearMonthCN(this.filterYearMonth))
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor($r("app.color.text_primary"))
          .margin({ left: 12, right: 12 })
        Image($r("sys.media.ohos_ic_public_arrow_right"))
          .width(18)
          .height(18)
          .onClick(() => {
            this.onNextMonth();
          })
      }
      .justifyContent(FlexAlign.Center)
      .width("100%")
      .margin({ bottom: 10 })

      // 类型筛选
      Row() {
        Button("全部")
          .fontSize(12)
          .height(28)
          .borderRadius(14)
          .backgroundColor(!this.filterType ? $r("app.color.primary") : $r("app.color.bg_page"))
          .fontColor(!this.filterType ? Color.White : $r("app.color.text_secondary"))
          .onClick(() => {
            this.filterType = "";
            this.refreshData();
          })
        Button("支出")
          .fontSize(12)
          .height(28)
          .borderRadius(14)
          .backgroundColor(
            this.filterType === "expense" ? $r("app.color.danger") : $r("app.color.bg_page"),
          )
          .fontColor(this.filterType === "expense" ? Color.White : $r("app.color.text_secondary"))
          .margin({ left: 8 })
          .onClick(() => {
            this.onTypeFilter("expense");
          })
        Button("收入")
          .fontSize(12)
          .height(28)
          .borderRadius(14)
          .backgroundColor(
            this.filterType === "income" ? $r("app.color.success") : $r("app.color.bg_page"),
          )
          .fontColor(this.filterType === "income" ? Color.White : $r("app.color.text_secondary"))
          .margin({ left: 8 })
          .onClick(() => {
            this.onTypeFilter("income");
          })
      }.width("100%")

      // 搜索框
      Row() {
        TextInput({ placeholder: "搜索备注关键词...", text: this.filterKeyword })
          .fontSize(13)
          .height(36)
          .backgroundColor($r("app.color.bg_page"))
          .borderRadius(18)
          .layoutWeight(1)
          .onChange((value: string) => {
            this.filterKeyword = value;
          })
          .onSubmit(() => {
            this.refreshData();
          })
        Button("搜索")
          .fontSize(12)
          .height(28)
          .backgroundColor($r("app.color.primary"))
          .fontColor(Color.White)
          .borderRadius(14)
          .margin({ left: 8 })
          .onClick(() => {
            this.refreshData();
          })
      }
      .width("100%")
      .margin({ top: 8 })
    }
    .width("100%")
    .padding({ left: 16, right: 16, top: 8, bottom: 10 })
    .backgroundColor($r("app.color.bg_card"));
  }

  private getCategoryName(categoryId: number): string {
    const cat = this.categories.find((c) => c.id === categoryId);
    return cat ? cat.name : "";
  }
}
