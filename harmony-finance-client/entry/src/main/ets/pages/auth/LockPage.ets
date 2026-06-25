/**
 * 应用密码锁验证页
 * 每次进入应用时拦截验证，通过后方可进入主页
 * @author 胡宪棋 软件2413 202421332084
 */
import router from "@ohos.router";
import { AppState } from "../../store/AppState";
import { StorageHelper } from "../../utils/storage";

@Entry
@Component
struct LockPage {
  @StorageLink("themeMode") themeMode: string = "light";
  @State passcode: string = "";
  @State errorMsg: string = "";
  @State isShaking: boolean = false;
  @State isLoading: boolean = true;
  @State passcodeLength: number = 6;
  @State dotIndices: number[] = [0, 1, 2, 3, 4, 5];
  private storedPasscode: string = "";

  async aboutToAppear(): Promise<void> {
    // 兜底：未登录直接跳转登录页
    if (!AppState.isLoggedIn) {
      router.replaceUrl({ url: "pages/auth/LoginPage" });
      return;
    }
    const enabled = await StorageHelper.getPasscodeEnabled();
    if (!enabled) {
      router.replaceUrl({ url: "pages/index/MainPage" });
      return;
    }
    this.storedPasscode = await StorageHelper.getPasscodeValue();
    this.passcodeLength = this.storedPasscode.length;
    this.dotIndices = [];
    for (let i = 0; i < this.passcodeLength; i++) {
      this.dotIndices.push(i);
    }
    this.isLoading = false;
  }

  onDigit(d: string): void {
    if (this.passcode.length >= this.passcodeLength) return;
    this.passcode += d;
    this.errorMsg = "";

    if (this.passcode.length === this.storedPasscode.length) {
      this.verify();
    }
  }

  onDelete(): void {
    if (this.passcode.length > 0) {
      this.passcode = this.passcode.substring(0, this.passcode.length - 1);
      this.errorMsg = "";
    }
  }

  verify(): void {
    if (this.passcode === this.storedPasscode) {
      router.replaceUrl({ url: "pages/index/MainPage" });
    } else {
      this.errorMsg = "密码错误，请重试";
      this.isShaking = true;
      this.passcode = "";
      setTimeout(() => {
        this.isShaking = false;
      }, 500);
    }
  }

  build() {
    Column() {
      if (this.isLoading) {
        Row() {
          LoadingProgress()
            .width(36)
            .height(36)
            .color($r("app.color.primary"))
        }
        .width("100%")
        .height("80%")
        .justifyContent(FlexAlign.Center);
      }
      if (!this.isLoading) {
        Column() {
          Text("🔒")
            .fontSize(48)
            .margin({ bottom: 20 })
          Text("请输入密码")
            .fontSize(18)
            .fontWeight(FontWeight.Medium)
            .fontColor($r("app.color.text_primary"))
            .margin({ bottom: 24 })
          Row() {
            ForEach(this.dotIndices, (i: number) => {
              Circle()
                .width(14)
                .height(14)
                .fill(i < this.passcode.length ? $r("app.color.primary") : $r("app.color.divider"))
                .margin({ left: 8, right: 8 });
            })
          }
          .margin({ bottom: 8 })
          Text(this.errorMsg)
            .fontSize(13)
            .fontColor("#FF4444")
            .height(20)
            .opacity(this.errorMsg ? 1 : 0)
            .margin({ bottom: 24 })
          Column() {
            Row() {
              this.digitButton("1")
              this.digitButton("2")
              this.digitButton("3")
            }
            Row() {
              this.digitButton("4")
              this.digitButton("5")
              this.digitButton("6")
            }
            Row() {
              this.digitButton("7")
              this.digitButton("8")
              this.digitButton("9")
            }
            Row() {
              this.emptyButton()
              this.digitButton("0")
              this.deleteButton()
            }
          }
          .width("100%")
          .margin({ top: 12 })
        }
        .width("100%")
        .padding({ left: 32, right: 32 })
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center);
      }
    }
    .width("100%")
    .height("100%")
    .justifyContent(FlexAlign.Center)
    .backgroundColor($r("app.color.bg_page"));
  }

  @Builder
  digitButton(digit: string) {
    Button(digit)
      .fontSize(24)
      .fontWeight(FontWeight.Medium)
      .fontColor($r("app.color.text_primary"))
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
      .width(64)
      .height(56)
      .margin(6)
      .onClick(() => {
        this.onDigit(digit);
      });
  }

  @Builder
  deleteButton() {
    Button("⌫")
      .fontSize(18)
      .fontColor($r("app.color.text_secondary"))
      .backgroundColor($r("app.color.bg_card"))
      .borderRadius(12)
      .width(64)
      .height(56)
      .margin(6)
      .onClick(() => {
        this.onDelete();
      });
  }

  @Builder
  emptyButton() {
    Column().width(64).height(56).margin(6);
  }
}
