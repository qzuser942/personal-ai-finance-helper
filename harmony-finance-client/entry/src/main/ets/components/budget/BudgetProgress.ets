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

  private getBarColor(): ResourceColor {
    const pct = this.getPercent();
    if (pct >= 100) return $r('app.color.danger');
    if (pct >= 80) return $r('app.color.warning');
    if (pct >= 50) return $r('app.color.warning');
    return $r('app.color.success');
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
          .fontColor($r('app.color.text_primary'))
          .layoutWeight(1)

        Text(`${this.spentAmount.toFixed(0)} / ${this.budgetAmount.toFixed(0)}`)
          .fontSize(12)
          .fontColor($r('app.color.text_hint'))

        if (this.alertLevel) {
          Text(this.getAlertText())
            .fontSize(11)
            .fontColor(this.alertLevel === 'OVER' ? $r('app.color.danger') : $r('app.color.warning'))
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
