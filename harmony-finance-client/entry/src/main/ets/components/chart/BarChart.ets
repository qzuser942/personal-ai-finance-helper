/**
 * 柱状图组件（Canvas实现）
 * 展示每日收支趋势
 * @author 胡宪棋 软件2413 202421332084
 */

export interface BarChartData {
  label: string
  income: number
  expense: number
}

@Component
export struct BarChart {
  @Prop data: BarChartData[] = [];
  @State canvasWidth: number = 340;
  @State canvasHeight: number = 240;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private context: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  build() {
    Column() {
      if (this.data && this.data.length > 0) {
        Canvas(this.context)
          .width(this.canvasWidth)
          .height(this.canvasHeight)
          .onReady(() => {
            this.drawBarChart();
          })

        // 图例
        Row() {
          Row() {
            Rect().width(12).height(12).fill('#4CAF50').radius(2)
            Text('收入').fontSize(11).fontColor('#999999').margin({ left: 4 })
          }.margin({ right: 16 })

          Row() {
            Rect().width(12).height(12).fill('#FF6B6B').radius(2)
            Text('支出').fontSize(11).fontColor('#999999').margin({ left: 4 })
          }
        }
        .justifyContent(FlexAlign.Center)
        .margin({ top: 8 })
      } else {
        Text('暂无数据')
          .fontSize(14)
          .fontColor('#999999')
          .width('100%')
          .height(200)
          .textAlign(TextAlign.Center)
      }
    }
    .width('100%')
  }

  private drawBarChart(): void {
    const ctx = this.context;
    const padTop = 20;
    const padRight = 10;
    const padBottom = 30;
    const padLeft = 40;
    const chartW = this.canvasWidth - padLeft - padRight;
    const chartH = this.canvasHeight - padTop - padBottom;

    // 找最大值
    let maxVal = 0;
    this.data.forEach(d => {
      maxVal = Math.max(maxVal, d.income, d.expense);
    });
    maxVal = Math.ceil(maxVal * 1.2);

    if (maxVal <= 0) maxVal = 1;

    const barWidth = Math.min(chartW / this.data.length * 0.35, 12);
    const gap = chartW / this.data.length;

    // Y轴刻度线
    ctx.strokeStyle = '#EEEEEE';
    ctx.lineWidth = 0.5;
    for (let i = 0; i <= 4; i++) {
      const y = padTop + (chartH / 4) * i;
      ctx.beginPath();
      ctx.moveTo(padLeft, y);
      ctx.lineTo(this.canvasWidth - padRight, y);
      ctx.stroke();

      // Y轴数值
      ctx.fillStyle = '#999999';
      ctx.font = '10px sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText((maxVal * (1 - i / 4)).toFixed(0), padLeft - 5, y + 4);
    }

    // 柱状图
    this.data.forEach((d, index) => {
      const centerX = padLeft + gap * (index + 0.5);

      // 收入柱（绿色）
      const incomeH = (d.income / maxVal) * chartH;
      ctx.fillStyle = '#4CAF50';
      ctx.fillRect(centerX - barWidth, padTop + chartH - incomeH, barWidth, incomeH);

      // 支出柱（红色）
      const expenseH = (d.expense / maxVal) * chartH;
      ctx.fillStyle = '#FF6B6B';
      ctx.fillRect(centerX + 1, padTop + chartH - expenseH, barWidth, expenseH);

      // X轴标签（每隔几个显示）
      if (index % Math.ceil(this.data.length / 10) === 0) {
        ctx.fillStyle = '#999999';
        ctx.font = '9px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText(d.label, centerX, this.canvasHeight - 5);
      }
    });
  }
}
