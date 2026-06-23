/**
 * 应用入口Ability
 * 个人智能理财系统 - 鸿蒙客户端
 * @author 胡宪棋 软件2413 202421332084
 */
import UIAbility from '@ohos.app.ability.UIAbility';
import window from '@ohos.window';
import { AppState } from '../store/AppState';
import { StorageHelper } from '../utils/storage';

export default class EntryAbility extends UIAbility {
  onCreate(want, launchParam) {
    // 初始化全局应用状态
    AppState.init(this.context);
    // 初始化Preferences存储
    StorageHelper.init(this.context);
  }

  onDestroy() {
  }

  onWindowStageCreate(windowStage: window.WindowStage) {
    windowStage.loadContent('pages/index/MainPage', (err, data) => {
      if (err.code) {
        return;
      }
    });

    windowStage.getMainWindow((err, data) => {
      if (err.code) {
        return;
      }
      // 设置全屏沉浸式
      let windowClass = data;
      windowClass.setWindowLayoutFullScreen(true);
    });
  }

  onWindowStageDestroy() {
  }

  onForeground() {
  }

  onBackground() {
  }
}
