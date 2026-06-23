/**
 * 空状态占位组件
 * @author 胡宪棋 软件2413 202421332084
 */
@Component
export struct EmptyView {
  @Prop message: string = '暂无数据';
  @Prop iconSize: number = 64;

  build() {
    Column() {
      Image($r('sys.media.ohos_ic_public_notes'))
        .width(this.iconSize)
        .height(this.iconSize)
        .opacity(0.3)

      Text(this.message)
        .fontSize(14)
        .fontColor('#999999')
        .margin({ top: 12 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
  }
}
