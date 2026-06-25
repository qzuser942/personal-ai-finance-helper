/**
 * 账单列表项组件
 * @author 胡宪棋 软件2413 202421332084
 */
import { BillListItem } from '../../model/BillModel';
import { DateUtil } from '../../utils/date';

@Component
export struct BillItem {
  @Prop bill: BillListItem;
  @Prop categoryName: string = '';
  @Prop iconName: string = '';
  onItemClick?: () => void;

  /** 分类图标颜色映射 */
  private getIconColor(type: string): ResourceColor {
    return type === 'income' ? $r('app.color.success') : $r('app.color.danger');
  }

  private getIconTag(categoryName: string): string {
    const map: Record<string, string> = {
      '餐饮': '🍽', '交通': '🚗', '购物': '🛒', '娱乐': '🎮',
      '住房': '🏠', '医疗': '🏥', '教育': '📚', '通讯': '📱',
      '服饰': '👔', '日用品': '🧴', '丽人': '💄', '运动': '⚽',
      '旅行': '✈', '宠物': '🐱', '数码': '💻', '其他支出': '📌',
      '工资': '💰', '奖金': '🏆', '投资收益': '📈', '兼职': '💼',
      '红包': '🧧', '退款': '↩', '其他收入': '📌'
    };
    return map[categoryName] || '📋';
  }

  build() {
    Row() {
      // 分类图标
      Text(this.getIconTag(this.categoryName || ''))
        .fontSize(28)
        .width(46)
        .height(46)
        .textAlign(TextAlign.Center)
        .borderRadius(12)
        .backgroundColor($r('app.color.bg_page'))

      // 中间信息区域
      Column() {
        Text(this.categoryName || this.bill.categoryId.toString())
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor($r('app.color.text_primary'))
        Text(this.bill.remark || '无备注')
          .fontSize(12)
          .fontColor($r('app.color.text_hint'))
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .margin({ top: 2 })
      }
      .alignItems(HorizontalAlign.Start)
      .margin({ left: 12 })
      .layoutWeight(1)

      // 右侧金额和时间
      Column() {
        Text(`${this.bill.type === 'income' ? '+' : '-'}${this.bill.amount.toFixed(2)}`)
          .fontSize(16)
          .fontWeight(FontWeight.Bold)
          .fontColor(this.bill.type === 'income' ? $r('app.color.success') : $r('app.color.text_primary'))

        Text(this.bill.consumeTime ? this.bill.consumeTime.substring(0, 10) : '')
          .fontSize(11)
          .fontColor($r('app.color.text_hint'))
          .margin({ top: 2 })
      }
      .alignItems(HorizontalAlign.End)
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 14, bottom: 14 })
    .backgroundColor($r('app.color.bg_card'))
    .borderRadius(8)
    .onClick(() => {
      if (this.onItemClick) {
        this.onItemClick();
      }
    })
  }
}
