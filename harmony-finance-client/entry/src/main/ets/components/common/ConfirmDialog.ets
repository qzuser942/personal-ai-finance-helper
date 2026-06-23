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
        .fontColor('#666666')
        .textAlign(TextAlign.Center)
        .margin({ bottom: 20 })
        .width('100%')

      Row() {
        Button(this.cancelText)
          .fontSize(14)
          .backgroundColor('#F0F0F0')
          .fontColor('#333333')
          .flexGrow(1)
          .onClick(() => {
            this.controller.close();
          })

        Button(this.confirmText)
          .fontSize(14)
          .backgroundColor('#4A90D9')
          .fontColor('#FFFFFF')
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
    .backgroundColor('#FFFFFF')
  }
}
