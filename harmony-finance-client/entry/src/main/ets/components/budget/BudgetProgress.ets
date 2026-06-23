/**
 * 预算进度条组件
 * @author 胡宪棋 软件2413 202421332084
 */
@Component
export struct BudgetProgress {
  @Prop categoryName: string = '';
  @Prop spentAmount: number = 0;
  @Prop budgetAmount: number = 0;
  @Prop alertLevel: string = ''; // 'WARNING' | 'OVER' | ''

  private getPercent(): number {
    if (this.budgetAmount <= 0) return 0;
    return Math.min((this.spentAmount / this.budgetAmount) * 100, 100);
  }

  private getBarColor(): string {
    const pct = this.getPercent();
    if (pct >= 100) return '#FF4444';
    if (pct >= 80) return '#FF9800';
    if (pct >= 50) return '#FFC107';
    return '#4CAF50';
  }

  private getAlertText(): string {
    if (this.alertLevel === 'OVER') return '已超出预算！';
    if (this.alertLevel === 'WARNING') return '即将超出预算';
    return '';
  }

  build() {
    Column() {
      Row() {
        Text(this.categoryName)
          .fontSize(14)
          .fontColor('#333333')
          .layoutWeight(1)

        Text(`${this.spentAmount.toFixed(0)} / ${this.budgetAmount.toFixed(0)}`)
          .fontSize(12)
          .fontColor('#999999')

        if (this.alertLevel) {
          Text(this.getAlertText())
            .fontSize(11)
            .fontColor(this.alertLevel === 'OVER' ? '#FF4444' : '#FF9800')
            .margin({ left: 6 })
        }
      }
      .width('100%')

      // 进度条
      Progress({
        value: this.getPercent(),
        total: 100,
        type: ProgressType.Linear
      })
        .width('100%')
        .height(8)
        .color(this.getBarColor())
        .style({ strokeWidth: 8, strokeRadius: 4 })
    }
    .width('100%')
    .padding(8)
  }
}
