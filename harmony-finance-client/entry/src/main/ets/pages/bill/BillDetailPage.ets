/**
 * 账单详情/编辑页
 * @author 胡宪棋 软件2413 202421332084
 */
import router from '@ohos.router';
import { BillApi } from '../../network/api/billApi';
import { CategoryApi } from '../../network/api/categoryApi';
import { FileApi } from '../../network/api/fileApi';
import { BillDetail, BillAddRequest } from '../../model/BillModel';
import { Category } from '../../model/CategoryModel';
import { DateUtil } from '../../utils/date';
import { ToastUtil } from '../../utils/toast';

@Entry
@Component
struct BillDetailPage {
  @State bill: BillDetail | null = null;
  @State categories: Category[] = [];
  @State isLoading: boolean = true;
  @State isEditing: boolean = false;

  // 编辑表单
  @State editAmount: string = '';
  @State editType: string = '';
  @State editCategoryId: number = 0;
  @State editRemark: string = '';
  @State editConsumeTime: string = '';

  private billId: number = 0;

  async aboutToAppear(): Promise<void> {
    const params = router.getParams() as Record<string, number>;
    this.billId = params?.['billId'] || 0;
    await this.loadData();
  }

  async loadData(): Promise<void> {
    this.isLoading = true;
    const [billRes, catRes] = await Promise.all([
      BillApi.getDetail(this.billId),
      CategoryApi.getList()
    ]);

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
    this.editRemark = this.bill.remark || '';
    this.editConsumeTime = this.bill.consumeTime;
    this.isEditing = true;
  }

  async onSaveEdit(): Promise<void> {
    const amt = parseFloat(this.editAmount);
    if (!amt || amt <= 0) {
      ToastUtil.showError('请输入有效金额');
      return;
    }

    const res = await BillApi.update(this.billId, {
      amount: amt,
      categoryId: this.editCategoryId,
      remark: this.editRemark,
      consumeTime: this.editConsumeTime
    });

    if (res.code === 200) {
      ToastUtil.showSuccess('修改成功');
      this.isEditing = false;
      await this.loadData();
    } else {
      ToastUtil.showError(res.message || '修改失败');
    }
  }

  async onDelete(): Promise<void> {
    const res = await BillApi.delete(this.billId);
    if (res.code === 200) {
      ToastUtil.showSuccess('已删除');
      router.back();
    } else {
      ToastUtil.showError(res.message || '删除失败');
    }
  }

  build() {
    Column() {
      // 顶部导航
      Row() {
        Image($r('sys.media.ohos_ic_public_arrow_left'))
          .width(24).height(24)
          .onClick(() => { router.back(); })
        Text('账单详情')
          .fontSize(18).fontWeight(FontWeight.Bold).fontColor('#333333').margin({ left: 8 })
        Blank()
        if (!this.isEditing) {
          Text('编辑')
            .fontSize(14).fontColor('#4A90D9')
            .onClick(() => { this.onEdit(); })
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 40, bottom: 10 })
      .backgroundColor('#FFFFFF')

      if (this.isLoading) {
        Column() {
          LoadingProgress().width(40).height(40).color('#4A90D9')
          Text('加载中...').fontSize(14).fontColor('#999999').margin({ top: 12 })
        }
        .width('100%').layoutWeight(1).justifyContent(FlexAlign.Center)
      } else if (this.bill) {
        Scroll() {
          Column() {
            if (this.isEditing) {
              this.buildEditForm()
            } else {
              this.buildDetailView()
            }
          }
          .padding({ left: 16, right: 16, bottom: 30 })
        }
        .layoutWeight(1)
      }
    }
    .width('100%').height('100%').backgroundColor('#F5F5F5')
  }

  @Builder
  buildDetailView() {
    Column() {
      // 金额卡片
      Column() {
        Text(this.bill!.type === 'income' ? '收入金额' : '支出金额')
          .fontSize(12).fontColor('#FFFFFF').opacity(0.8)
        Text(`${this.bill!.type === 'income' ? '+' : '-'}${this.bill!.amount.toFixed(2)}`)
          .fontSize(40).fontWeight(FontWeight.Bold).fontColor('#FFFFFF').margin({ top: 4 })
      }
      .width('100%')
      .padding(24)
      .borderRadius(16)
      .backgroundColor(this.bill!.type === 'income' ? '#4CAF50' : '#FF6B6B')

      // 详情字段
      this.buildDetailItem('收支类型', this.bill!.type === 'income' ? '💰 收入' : '💸 支出')
      this.buildDetailItem('消费分类', this.bill!.categoryName)
      this.buildDetailItem('备注', this.bill!.remark || '无')
      this.buildDetailItem('消费时间', this.bill!.consumeTime)
      this.buildDetailItem('创建时间', this.bill!.createdAt)

      if (this.bill!.receiptImage) {
        this.buildDetailItem('小票图片', '')
        Image(FileApi.getImageUrl(this.bill!.receiptImage))
          .width('100%')
          .objectFit(ImageFit.Contain)
          .borderRadius(8)
          .margin({ top: 8 })
      }

      // 操作按钮
      Row() {
        Button('🗑 删除').fontSize(14)
          .backgroundColor('#FF6B6B').fontColor('#FFFFFF')
          .borderRadius(20).height(42).flexGrow(1)
          .onClick(() => { this.onDelete(); })
      }
      .width('100%').margin({ top: 30 })
    }
  }

  @Builder
  buildEditForm() {
    Column() {
      // 金额
      this.buildEditLabel('金额')
      TextInput({ text: this.editAmount })
        .type(InputType.Number).fontSize(18).fontWeight(FontWeight.Bold)
        .height(48).backgroundColor('#FFFFFF').borderRadius(8)
        .width('100%').padding({ left: 12, right: 12 })
        .onChange((v: string) => { this.editAmount = v; })

      // 分类
      this.buildEditLabel('分类')
      Grid() {
        ForEach(this.categories, (cat: Category) => {
          GridItem() {
            Column() {
              Text(this.getCategoryIcon(cat.name)).fontSize(24)
              Text(cat.name).fontSize(11).fontColor('#666666').margin({ top: 2 })
            }
            .width('100%').height(60).justifyContent(FlexAlign.Center)
            .borderRadius(8)
            .backgroundColor(this.editCategoryId === cat.id ? '#E3F2FD' : '#F5F5F5')
          }
          .onClick(() => { this.editCategoryId = cat.id; })
        })
      }
      .columnsTemplate('1fr 1fr 1fr 1fr')
      .rowsGap(6).columnsGap(6)
      .width('100%').margin({ top: 4 })

      // 备注
      this.buildEditLabel('备注')
      TextInput({ text: this.editRemark })
        .fontSize(14).height(44).backgroundColor('#FFFFFF').borderRadius(8)
        .width('100%').padding({ left: 12, right: 12 }).maxLength(500)
        .onChange((v: string) => { this.editRemark = v; })

      // 时间
      this.buildEditLabel('消费时间')
      TextInput({ text: this.editConsumeTime })
        .fontSize(14).height(44).backgroundColor('#FFFFFF').borderRadius(8)
        .width('100%').padding({ left: 12, right: 12 })
        .onChange((v: string) => { this.editConsumeTime = v; })

      // 按钮
      Row() {
        Button('取消').fontSize(14)
          .backgroundColor('#F0F0F0').fontColor('#666666')
          .borderRadius(20).height(42).flexGrow(1)
          .onClick(() => { this.isEditing = false; })
        Button('保存').fontSize(14)
          .backgroundColor('#4A90D9').fontColor('#FFFFFF')
          .borderRadius(20).height(42).flexGrow(1).margin({ left: 10 })
          .onClick(() => { this.onSaveEdit(); })
      }
      .width('100%').margin({ top: 20 })
    }
  }

  @Builder
  buildDetailItem(label: string, value: string) {
    Row() {
      Text(label).fontSize(14).fontColor('#999999').width(70)
      Text(value).fontSize(14).fontColor('#333333').layoutWeight(1)
    }
    .width('100%')
    .padding({ top: 14, bottom: 14, left: 16, right: 16 })
    .backgroundColor('#FFFFFF')
    .borderRadius(8)
    .margin({ top: 10 })
  }

  @Builder
  buildEditLabel(label: string) {
    Text(label).fontSize(12).fontColor('#999999').width('100%').margin({ top: 14, bottom: 6 })
  }

  private getCategoryIcon(name: string): string {
    const map: Record<string, string> = {
      '餐饮': '🍽️', '交通': '🚗', '购物': '🛒', '娱乐': '🎮', '住房': '🏠',
      '医疗': '🏥', '教育': '📚', '通讯': '📱', '服饰': '👔', '日用品': '🧴',
      '丽人': '💄', '运动': '⚽', '旅行': '✈️', '宠物': '🐱', '数码': '💻',
      '其他支出': '📌', '工资': '💰', '奖金': '🏆', '投资收益': '📈',
      '兼职': '💼', '红包': '🧧', '退款': '↩️', '其他收入': '📌'
    };
    return map[name] || '📋';
  }
}
