/**
 * 确认弹窗组件
 * @author 胡宪棋 软件2413 202421332084
 */
@CustomDialog
export struct ConfirmDialog {
  controller: CustomDialogController;
  title: string = '提示';
  message: string = '';
  confirmText: string = '确认';
  cancelText: string = '取消';
  onConfirm?: () => void;

  build() {
    Column() {
      Text(this.title)
        .fontSize(18)
        .fontWeight(FontWeight.Bold)
        .margin({ bottom: 12 })

      Text(this.message)
        .fontSize(14)
        .fontColor($r('app.color.text_secondary'))
        .textAlign(TextAlign.Center)
        .margin({ bottom: 20 })
        .width('100%')

      Row() {
        Button(this.cancelText)
          .fontSize(14)
          .backgroundColor($r('app.color.bg_page'))
          .fontColor($r('app.color.text_primary'))
          .flexGrow(1)
          .onClick(() => {
            this.controller.close();
          })

        Button(this.confirmText)
          .fontSize(14)
          .backgroundColor($r('app.color.primary'))
          .fontColor(Color.White)
          .flexGrow(1)
          .margin({ left: 10 })
          .onClick(() => {
            if (this.onConfirm) {
              this.onConfirm();
            }
            this.controller.close();
          })
      }
      .width('100%')
    }
    .width('75%')
    .padding(24)
    .borderRadius(12)
    .backgroundColor($r('app.color.bg_card'))
  }
}
