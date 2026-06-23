/**
 * 加载中弹窗组件
 * @author 胡宪棋 软件2413 202421332084
 */
@CustomDialog
export struct LoadingDialog {
  controller: CustomDialogController;
  message: string = '加载中...';

  build() {
    Column() {
      LoadingProgress()
        .width(40)
        .height(40)
        .color('#4A90D9')
      Text(this.message)
        .fontSize(14)
        .fontColor('#666666')
        .margin({ top: 12 })
    }
    .width('60%')
    .padding(30)
    .borderRadius(12)
    .backgroundColor('#FFFFFF')
  }
}
