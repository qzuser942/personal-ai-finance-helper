/**
 * 用户注册页面
 * @author 胡宪棋 软件2413 202421332084
 */
import router from '@ohos.router';
import { AuthApi } from '../../network/api/authApi';
import { ToastUtil } from '../../utils/toast';

@Entry
@Component
struct RegisterPage {
  @State username: string = '';
  @State password: string = '';
  @State confirmPassword: string = '';
  @State isLoading: boolean = false;

  async onRegister(): Promise<void> {
    // 前端校验
    if (!this.username.trim()) {
      ToastUtil.showError('请输入用户名');
      return;
    }
    if (this.username.trim().length < 4 || this.username.trim().length > 20) {
      ToastUtil.showError('用户名需4-20字符');
      return;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(this.username.trim())) {
      ToastUtil.showError('用户名仅支持字母、数字、下划线');
      return;
    }
    if (!this.password) {
      ToastUtil.showError('请输入密码');
      return;
    }
    if (this.password.length < 6 || this.password.length > 20) {
      ToastUtil.showError('密码需6-20字符');
      return;
    }
    if (this.password !== this.confirmPassword) {
      ToastUtil.showError('两次密码输入不一致');
      return;
    }

    this.isLoading = true;
    try {
      const res = await AuthApi.register({
        username: this.username.trim(),
        password: this.password,
        confirmPassword: this.confirmPassword
      });

      if (res.code === 200) {
        ToastUtil.showSuccess('注册成功，请登录');
        router.back();
      } else if (res.code === 10001) {
        ToastUtil.showError('用户名已存在，请更换');
      } else {
        ToastUtil.showError(res.message || '注册失败');
      }
    } catch (err) {
      ToastUtil.showError('注册异常，请重试');
    } finally {
      this.isLoading = false;
    }
  }

  build() {
    Column() {
      // 顶部标题栏
      Row() {
        Image($r('sys.media.ohos_ic_public_arrow_left'))
          .width(24)
          .height(24)
          .onClick(() => {
            router.back();
          })
        Text('创建账号')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor('#333333')
          .margin({ left: 8 })
      }
      .width('100%')
      .padding({ left: 16, top: 44, bottom: 20 })
      .backgroundColor('#FFFFFF')

      Column() {
        // 用户名
        TextInput({ placeholder: '用户名（4-20位字母/数字/下划线）', text: this.username })
          .width('100%')
          .height(48)
          .fontSize(15)
          .backgroundColor('#F5F5F5')
          .borderRadius(8)
          .onChange((value: string) => { this.username = value; })

        // 密码
        TextInput({ placeholder: '密码（6-20位）', text: this.password })
          .width('100%')
          .height(48)
          .type(InputType.Password)
          .fontSize(15)
          .backgroundColor('#F5F5F5')
          .borderRadius(8)
          .margin({ top: 16 })
          .onChange((value: string) => { this.password = value; })

        // 确认密码
        TextInput({ placeholder: '确认密码', text: this.confirmPassword })
          .width('100%')
          .height(48)
          .type(InputType.Password)
          .fontSize(15)
          .backgroundColor('#F5F5F5')
          .borderRadius(8)
          .margin({ top: 16 })
          .onChange((value: string) => { this.confirmPassword = value; })

        // 注册按钮
        Button(this.isLoading ? '注册中...' : '注 册')
          .width('100%')
          .height(48)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .backgroundColor('#4A90D9')
          .borderRadius(24)
          .enabled(!this.isLoading)
          .margin({ top: 30 })
          .onClick(() => { this.onRegister(); })

        // 返回登录
        Row() {
          Text('已有账号？')
            .fontSize(13)
            .fontColor('#999999')
          Text('返回登录')
            .fontSize(13)
            .fontColor('#4A90D9')
            .margin({ left: 4 })
            .onClick(() => { router.back(); })
        }
        .margin({ top: 20 })
      }
      .width('85%')
      .margin({ top: 40 })
      .alignItems(HorizontalAlign.Center)
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#FFFFFF')
  }
}
