/**
 * 饼图组件（Canvas实现）
 * 展示支出分类占比
 * @author 胡宪棋 软件2413 202421332084
 */

export interface PieChartData {
  name: string
  value: number
  color: string
}

@Component
export struct PieChart {
  @Prop data: PieChartData[] = [];
  @State canvasWidth: number = 300;
  @State canvasHeight: number = 300;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private context: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  private readonly COLORS: string[] = [
    '#4A90D9', '#FF6B6B', '#4CAF50', '#FF9800', '#9C27B0',
    '#00BCD4', '#FFEB3B', '#795548', '#607D8B', '#E91E63',
    '#3F51B5', '#009688', '#FFC107', '#8BC34A', '#FF5722'
  ];

  build() {
    Column() {
      if (this.data && this.data.length > 0) {
        Canvas(this.context)
          .width(this.canvasWidth)
          .height(this.canvasHeight)
          .onReady(() => {
            this.drawPieChart();
          })
      } else {
        Text('暂无数据')
          .fontSize(14)
          .fontColor('#999999')
          .width('100%')
          .height(200)
          .textAlign(TextAlign.Center)
      }

      // 图例
      if (this.data && this.data.length > 0) {
        Column() {
          ForEach(this.data, (item: PieChartData, index: number) => {
            Row() {
              Rect()
                .width(10)
                .height(10)
                .fill(item.color || this.COLORS[index % this.COLORS.length])
                .radius(2)
              Text(item.name)
                .fontSize(12)
                .fontColor('#666666')
                .margin({ left: 4 })
              Text(`${item.value.toFixed(2)}`)
                .fontSize(12)
                .fontColor('#999999')
                .margin({ left: 4 })
            }
            .margin({ top: 4 })
            .width('100%')
          })
        }
        .width('100%')
        .margin({ top: 16 })
        .padding({ left: 16, right: 16 })
      }
    }
    .width('100%')
  }

  private drawPieChart(): void {
    const ctx = this.context;
    const total = this.data.reduce((sum, item) => sum + item.value, 0);
    if (total <= 0) return;

    const cx = this.canvasWidth / 2;
    const cy = this.canvasHeight / 2;
    const radius = Math.min(cx, cy) - 10;
    let startAngle = -Math.PI / 2;

    this.data.forEach((item, index) => {
      const sliceAngle = (item.value / total) * 2 * Math.PI;
      const color = item.color || this.COLORS[index % this.COLORS.length];

      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, radius, startAngle, startAngle + sliceAngle);
      ctx.closePath();
      ctx.fillStyle = color;
      ctx.fill();

      // 绘制百分比文字
      const midAngle = startAngle + sliceAngle / 2;
      const labelR = radius * 0.65;
      const labelX = cx + Math.cos(midAngle) * labelR;
      const labelY = cy + Math.sin(midAngle) * labelR;
      const pct = ((item.value / total) * 100).toFixed(1);

      ctx.font = '12px sans-serif';
      ctx.fillStyle = '#FFFFFF';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      if (parseFloat(pct) > 3) {
        ctx.fillText(`${pct}%`, labelX, labelY);
      }

      startAngle += sliceAngle;
    });
  }
}
