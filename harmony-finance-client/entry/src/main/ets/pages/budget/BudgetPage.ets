/**
 * 预算与存钱目标页面
 * 功能：月度预算展示、预算预警、存钱目标创建/追踪/归档
 * @author 胡宪棋 软件2413 202421332084
 */
import router from '@ohos.router';
import { BudgetApi } from '../../network/api/budgetApi';
import { SaveTargetApi } from '../../network/api/saveTargetApi';
import { CategoryApi } from '../../network/api/categoryApi';
import { DateUtil } from '../../utils/date';
import { ToastUtil } from '../../utils/toast';
import { BudgetData, BudgetCreateRequest } from '../../model/BudgetModel';
import { SaveTarget } from '../../model/SaveTargetModel';
import { Category } from '../../model/CategoryModel';
import { BudgetProgress } from '../../components/budget/BudgetProgress';
import { EmptyView } from '../../components/common/EmptyView';

@Entry
@Component
struct BudgetPage {
  // 预算
  @State budget: BudgetData | null = null;
  @State categories: Category[] = [];
  @State showBudgetSetting: boolean = false;
  @State budgetTotalInput: string = '';
  @State categoryBudgetInputs: Record<string, string> = {};

  // 存钱目标
  @State targets: SaveTarget[] = [];
  @State targetFilter: number = 0; // 0-进行中 1-已完成
  @State showCreateTarget: boolean = false;
  @State newTargetName: string = '';
  @State newTargetAmount: string = '';
  @State showAddAmount: Record<string, boolean> = {};
  @State addAmountInput: Record<string, string> = {};
  @State isLoading: boolean = false;

  async aboutToAppear(): Promise<void> {
    this.isLoading = true;
    await Promise.all([
      this.loadBudget(),
      this.loadTargets(),
      this.loadCategories()
    ]);
    this.isLoading = false;
  }

  async loadBudget(): Promise<void> {
    const res = await BudgetApi.getCurrent();
    if (res.code === 200 && res.data) {
      this.budget = res.data;
      // 检查预算预警
      this.checkBudgetAlerts(res.data);
    }
  }

  async loadCategories(): Promise<void> {
    const res = await CategoryApi.getList('expense');
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
        if (alert.alertLevel === 'OVER') {
          ToastUtil.showWarning(`${alert.categoryName}预算已超出！`);
        } else if (alert.alertLevel === 'WARNING') {
          ToastUtil.showWarning(`${alert.categoryName}预算即将超出（已达${((alert.spentAmount/alert.budgetAmount)*100).toFixed(0)}%）`);
        }
      }
    }
  }

  async onSaveBudget(): Promise<void> {
    const total = parseFloat(this.budgetTotalInput);
    if (!total || total <= 0) {
      ToastUtil.showError('请输入有效预算金额');
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
      categoryBudgets: Object.keys(catBudgets).length > 0 ? catBudgets : undefined
    };

    const res = await BudgetApi.createOrUpdate(data);
    if (res.code === 200 && res.data) {
      this.budget = res.data;
      this.showBudgetSetting = false;
      ToastUtil.showSuccess('预算设置成功');
      this.checkBudgetAlerts(res.data);
    } else {
      ToastUtil.showError(res.message || '预算设置失败');
    }
  }

  async onCreateTarget(): Promise<void> {
    const amount = parseFloat(this.newTargetAmount);
    if (!this.newTargetName.trim()) { ToastUtil.showError('请输入目标名称'); return; }
    if (!amount || amount <= 0) { ToastUtil.showError('请输入有效金额'); return; }

    const res = await SaveTargetApi.create({
      name: this.newTargetName.trim(),
      targetAmount: amount
    });

    if (res.code === 200) {
      ToastUtil.showSuccess('目标创建成功');
      this.showCreateTarget = false;
      this.newTargetName = '';
      this.newTargetAmount = '';
      await this.loadTargets();
    } else {
      ToastUtil.showError(res.message || '创建失败');
    }
  }

  async onAddAmount(targetId: number, amount: number): Promise<void> {
    if (!amount || amount <= 0) { ToastUtil.showError('请输入有效金额'); return; }
    const res = await SaveTargetApi.update(targetId, { addAmount: amount });
    if (res.code === 200) {
      ToastUtil.showSuccess('存款成功！');
      this.showAddAmount[targetId.toString()] = false;
      await this.loadTargets();
    } else {
      ToastUtil.showError(res.message || '操作失败');
    }
  }

  async onDeleteTarget(id: number): Promise<void> {
    const res = await SaveTargetApi.delete(id);
    if (res.code === 200) { ToastUtil.showSuccess('已删除'); this.loadTargets(); }
    else { ToastUtil.showError(res.message || '删除失败'); }
  }

  build() {
    Column() {
      this.buildTopBar();
      if (this.isLoading) {
        Column() {
          LoadingProgress().width(40).height(40).color('#4A90D9')
        }.width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            this.buildBudgetSection();
            this.buildTargetSection();
          }.padding({ left: 16, right: 16, bottom: 30 })
        }.layoutWeight(1).scrollBar(BarState.Off)
      }
    }
    .width('100%').height('100%').backgroundColor('#F5F5F5')
  }

  @Builder
  buildTopBar() {
    Row() {
      Image($r('sys.media.ohos_ic_public_arrow_left'))
        .width(24).height(24).onClick(() => { router.back(); })
      Text('预算与目标').fontSize(18).fontWeight(FontWeight.Bold).fontColor('#333333').margin({ left: 8 })
      Blank()
    }
    .width('100%').padding({ left: 16, right: 16, top: 40, bottom: 10 }).backgroundColor('#FFFFFF')
  }

  @Builder
  buildBudgetSection() {
    Column() {
      // 标题行
      Row() {
        Text('💵 月度预算').fontSize(16).fontWeight(FontWeight.Bold).fontColor('#333333')
        Blank()
        Text(this.budget ? '修改预算' : '设置预算').fontSize(13).fontColor('#4A90D9')
          .onClick(() => {
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
            this.showBudgetSetting = !this.showBudgetSetting;
          })
      }.width('100%').margin({ bottom: 12 })

      if (this.budget && !this.showBudgetSetting) {
        // 预算概览
        Row() {
          Column() { Text('总预算').fontSize(11).fontColor('#999999')
            Text(DateUtil.formatMoney(this.budget.totalBudget)).fontSize(16).fontWeight(FontWeight.Bold).fontColor('#4CAF50')
          }.flexGrow(1).alignItems(HorizontalAlign.Center)
          Column() { Text('已消费').fontSize(11).fontColor('#999999')
            Text(DateUtil.formatMoney(this.budget.totalSpent)).fontSize(16).fontWeight(FontWeight.Bold).fontColor('#FF9800')
          }.flexGrow(1).alignItems(HorizontalAlign.Center)
          Column() { Text('剩余').fontSize(11).fontColor('#999999')
            Text(DateUtil.formatMoney(this.budget.remaining)).fontSize(16).fontWeight(FontWeight.Bold).fontColor('#4A90D9')
          }.flexGrow(1).alignItems(HorizontalAlign.Center)
        }.width('100%').padding(16).backgroundColor('#FFFFFF').borderRadius(12).margin({ bottom: 12 })

        // 总进度条
        Text(`已使用 ${this.budget.usagePercent.toFixed(1)}%`).fontSize(11).fontColor('#999999').margin({ bottom: 4 }).width('100%')
        Progress({ value: this.budget.usagePercent, total: 100, type: ProgressType.Linear })
          .width('100%').height(10)
          .color(this.budget.usagePercent >= 100 ? '#FF4444' : this.budget.usagePercent >= 80 ? '#FF9800' : '#4CAF50')

        // 分类子预算进度
        if (this.budget.categoryBudgets) {
          Text('分类预算明细').fontSize(13).fontWeight(FontWeight.Medium).fontColor('#333333')
            .width('100%').margin({ top: 16, bottom: 8 })
          ForEach(Object.keys(this.budget.categoryBudgets), (key: string) => {
            const catId = parseInt(key);
            const budgetAmt = this.budget!.categoryBudgets![key] || 0;
            const spentAmt = this.budget!.categorySpent![key] || 0;
            const alert = this.budget!.alertCategories?.find(a => a.categoryId === catId);
            BudgetProgress({
              categoryName: this.categories.find(c => c.id === catId)?.name || `分类${catId}`,
              spentAmount: spentAmt,
              budgetAmount: budgetAmt,
              alertLevel: alert?.alertLevel || ''
            })
          })
        }
      } else if (this.showBudgetSetting) {
        // 预算设置表单
        Column() {
          Text('月度总预算').fontSize(12).fontColor('#999999').width('100%').margin({ bottom: 4 })
          TextInput({ text: this.budgetTotalInput }).type(InputType.Number)
            .fontSize(16).height(44).backgroundColor('#FFFFFF').borderRadius(8)
            .width('100%').padding({ left: 12, right: 12 })
            .onChange((v: string) => { this.budgetTotalInput = v; })

          Text('各分类子预算（可选）').fontSize(12).fontColor('#999999').width('100%').margin({ top: 12, bottom: 6 })
          ForEach(this.categories.filter(c => c.type === 'expense'), (cat: Category) => {
            Row() {
              Text(cat.name).fontSize(13).fontColor('#666666').width(60)
              TextInput({ text: this.categoryBudgetInputs[cat.id.toString()] || '' })
                .type(InputType.Number).fontSize(13).height(36).backgroundColor('#FFFFFF')
                .borderRadius(6).layoutWeight(1).padding({ left: 8, right: 8 })
                .onChange((v: string) => { this.categoryBudgetInputs[cat.id.toString()] = v; })
            }.width('100%').margin({ top: 4 })
          })

          Button('保存预算').fontSize(14)
            .backgroundColor('#4A90D9').fontColor('#FFFFFF').borderRadius(20)
            .height(40).width('100%').margin({ top: 16 })
            .onClick(() => { this.onSaveBudget(); })
        }.width('100%').padding(16).backgroundColor('#FFFFFF').borderRadius(12)
      } else {
        EmptyView({ message: '暂无预算，点击设置' })
      }
    }.width('100%').padding(16).backgroundColor('#FFFFFF').borderRadius(12).margin({ top: 16 })
  }

  @Builder
  buildTargetSection() {
    Column() {
      Row() {
        Text('🎯 存钱目标').fontSize(16).fontWeight(FontWeight.Bold).fontColor('#333333')
        Blank()
        Text('+ 新建').fontSize(13).fontColor('#4A90D9')
          .onClick(() => { this.showCreateTarget = !this.showCreateTarget; })
      }.width('100%').margin({ bottom: 12 })

      // 状态切换
      Row() {
        Button('进行中').fontSize(12).height(28).borderRadius(14)
          .backgroundColor(this.targetFilter === 0 ? '#4A90D9' : '#F0F0F0')
          .fontColor(this.targetFilter === 0 ? '#FFFFFF' : '#666666')
          .onClick(() => { this.targetFilter = 0; this.loadTargets(); })
        Button('已完成').fontSize(12).height(28).borderRadius(14)
          .backgroundColor(this.targetFilter === 1 ? '#4CAF50' : '#F0F0F0')
          .fontColor(this.targetFilter === 1 ? '#FFFFFF' : '#666666').margin({ left: 8 })
          .onClick(() => { this.targetFilter = 1; this.loadTargets(); })
      }.width('100%').margin({ bottom: 10 })

      // 新建目标表单
      if (this.showCreateTarget) {
        Column() {
          TextInput({ placeholder: '目标名称', text: this.newTargetName }).fontSize(14).height(40)
            .backgroundColor('#F5F5F5').borderRadius(8).width('100%').padding({ left: 10, right: 10 })
            .onChange((v: string) => { this.newTargetName = v; })
          TextInput({ placeholder: '目标金额', text: this.newTargetAmount }).type(InputType.Number)
            .fontSize(14).height(40).backgroundColor('#F5F5F5').borderRadius(8)
            .width('100%').padding({ left: 10, right: 10 }).margin({ top: 8 })
            .onChange((v: string) => { this.newTargetAmount = v; })
          Button('创建目标').fontSize(13).backgroundColor('#4A90D9').fontColor('#FFFFFF')
            .borderRadius(18).height(36).width('100%').margin({ top: 10 })
            .onClick(() => { this.onCreateTarget(); })
        }.width('100%').padding(12).backgroundColor('#F5F5F5').borderRadius(8).margin({ bottom: 10 })
      }

      // 目标列表
      if (this.targets.length === 0) {
        EmptyView({ message: this.targetFilter === 0 ? '暂无进行中的目标' : '暂无已完成的目标' })
      } else {
        ForEach(this.targets, (target: SaveTarget) => {
          Column() {
            Row() {
              Text(target.name).fontSize(15).fontWeight(FontWeight.Medium).fontColor('#333333').layoutWeight(1)
              if (target.status === 1) {
                Text('✅ 已完成').fontSize(11).fontColor('#4CAF50')
              }
              Text('🗑').fontSize(14).margin({ left: 8 })
                .onClick(() => { this.onDeleteTarget(target.id); })
            }.width('100%')

            Text(`目标：${DateUtil.formatMoney(target.targetAmount)}  |  已存：${DateUtil.formatMoney(target.savedAmount)}`)
              .fontSize(12).fontColor('#999999').margin({ top: 4 }).width('100%')

            Progress({ value: target.progressPercent, total: 100, type: ProgressType.Linear })
              .width('100%').height(8).margin({ top: 6 })
              .color(target.status === 1 ? '#4CAF50' : target.progressPercent >= 80 ? '#FF9800' : '#4A90D9')

            if (target.status === 0) {
              Row() {
                Button('💰 存入').fontSize(11).height(26).backgroundColor('#E8F5E9')
                  .fontColor('#4CAF50').borderRadius(13)
                  .onClick(() => {
                    this.showAddAmount[target.id.toString()] = !this.showAddAmount[target.id.toString()];
                  })
              }.width('100%').margin({ top: 6 })

              if (this.showAddAmount[target.id.toString()]) {
                Row() {
                  TextInput({ placeholder: '金额', text: this.addAmountInput[target.id.toString()] || '' })
                    .type(InputType.Number).fontSize(13).height(32).backgroundColor('#F5F5F5')
                    .borderRadius(16).layoutWeight(1).padding({ left: 10, right: 10 })
                    .onChange((v: string) => { this.addAmountInput[target.id.toString()] = v; })
                  Button('确认').fontSize(11).height(28)
                    .backgroundColor('#4CAF50').fontColor('#FFFFFF').borderRadius(14).margin({ left: 8 })
                    .onClick(() => {
                      const amt = parseFloat(this.addAmountInput[target.id.toString()]);
                      this.onAddAmount(target.id, amt);
                    })
                }.width('100%').margin({ top: 6 })
              }
            }
          }
          .width('100%').padding(12).backgroundColor('#FFFFFF').borderRadius(8).margin({ top: 8 })
        })
      }
    }.width('100%').padding(16).backgroundColor('#FFFFFF').borderRadius(12).margin({ top: 16 })
  }
}
