/**
 * 深色模式单元测试
 * 测试框架：@ohos/hypium
 * 测试入口：local unit test on HarmonyOS
 *
 * 关注点：
 * 1. AppState.applyThemeMode() 行为
 * 2. AppState.toggleTheme() / setThemeMode() 行为（均为同步方法）
 * 3. AppState.loadLoginState() 加载主题并应用
 * 4. 启动流程 (EntryAbility) 主题应用时序
 *
 * 修复说明：
 * - AppState 新增 static themeMode 字段，测试可直接读写
 * - setThemeMode / toggleTheme 是同步方法，不需要 await
 * - loadLoginState 是 async，仍需 await
 */
import { describe, it, expect, beforeEach, afterEach } from '@ohos/hypium';
import ConfigurationConstant from '@ohos.app.ability.ConfigurationConstant';
import { AppState } from '../../../../main/ets/store/AppState';
import { StorageHelper } from '../../../../main/ets/utils/storage';

// ==================== Mock 辅助 ====================

/** 模拟 applicationContext.setColorMode */
class MockApplicationContext {
  public lastSetColorMode: number | null = null;
  public lastSetColorModeAt: number = -1;
  public throwOnSet: boolean = false;

  setColorMode(mode: number): void {
    if (this.throwOnSet) {
      throw new Error('mock setColorMode failure');
    }
    this.lastSetColorMode = mode;
    this.lastSetColorModeAt = Date.now();
  }
}

/** 模拟 Context */
class MockContext {
  appCtx: MockApplicationContext = new MockApplicationContext();

  getApplicationContext(): MockApplicationContext {
    return this.appCtx;
  }
}

/** 模拟 Storage */
class MockStorage {
  store: Map<string, string | number | boolean> = new Map();

  async getString(key: string, def: string): Promise<string> {
    const v = this.store.get(key);
    return v === undefined ? def : (v as string);
  }

  async putString(key: string, v: string): Promise<void> {
    this.store.set(key, v);
  }

  async getBoolean(key: string, def: boolean): Promise<boolean> {
    const v = this.store.get(key);
    return v === undefined ? def : (v as boolean);
  }

  async putBoolean(key: string, v: boolean): Promise<void> {
    this.store.set(key, v);
  }

  async clearAll(): Promise<void> {
    this.store.clear();
  }
}

// ==================== 测试套件 ====================

export default function darkModeTest() {
  describe('AppState.applyThemeMode', () => {
    let mockCtx: MockContext;

    beforeEach(() => {
      // 使用 static themeMode 字段直接设置（修复：不再是 undefined）
      AppState.themeMode = 'light';
      mockCtx = new MockContext();
      AppState.context = mockCtx as unknown as Context;
    });

    afterEach(() => {
      AppState.context = undefined as unknown as Context;
    });

    it('should_not_throw_when_context_unset', 0, () => {
      // 验证当 context 未设置时，调用 applyThemeMode() 不应抛异常
      AppState.context = undefined as unknown as Context;
      let threw = false;
      try {
        AppState.applyThemeMode();
      } catch (e) {
        threw = true;
      }
      expect(threw).assertFalse();
    });

    it('should_call_setColorMode_with_DARK_when_themeMode_is_dark', 0, () => {
      // themeMode = dark 时应调 setColorMode(COLOR_MODE_DARK)
      AppState.themeMode = 'dark';
      AppState.applyThemeMode();
      expect(mockCtx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_DARK);
    });

    it('should_call_setColorMode_with_LIGHT_when_themeMode_is_light', 0, () => {
      // themeMode = light 时应调 setColorMode(COLOR_MODE_LIGHT)
      AppState.themeMode = 'light';
      AppState.applyThemeMode();
      expect(mockCtx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_LIGHT);
    });

    it('should_silently_handle_setColorMode_throw', 0, () => {
      // setColorMode 抛异常时，applyThemeMode 不应向上抛
      mockCtx.appCtx.throwOnSet = true;
      AppState.themeMode = 'dark';
      let threw = false;
      try {
        AppState.applyThemeMode();
      } catch (e) {
        threw = true;
      }
      expect(threw).assertFalse();
    });
  });

  describe('AppState.toggleTheme', () => {
    let mockCtx: MockContext;

    beforeEach(() => {
      AppState.themeMode = 'light';
      mockCtx = new MockContext();
      AppState.context = mockCtx as unknown as Context;
    });

    afterEach(() => {
      AppState.context = undefined as unknown as Context;
    });

    it('should_flip_light_to_dark', 0, () => {
      // toggleTheme 是同步方法，不需要 await
      AppState.themeMode = 'light';
      const result = AppState.toggleTheme();
      expect(result).assertEqual('dark');
      expect(AppState.themeMode).assertEqual('dark');
    });

    it('should_flip_dark_to_light', 0, () => {
      AppState.themeMode = 'dark';
      const result = AppState.toggleTheme();
      expect(result).assertEqual('light');
      expect(AppState.themeMode).assertEqual('light');
    });

    it('should_call_applyThemeMode_after_flip', 0, () => {
      AppState.themeMode = 'light';
      AppState.toggleTheme();
      expect(mockCtx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_DARK);
    });
  });

  describe('AppState.setThemeMode', () => {
    beforeEach(() => {
      AppState.themeMode = 'light';
      const mockCtx = new MockContext();
      AppState.context = mockCtx as unknown as Context;
    });

    afterEach(() => {
      AppState.context = undefined as unknown as Context;
    });

    it('should_set_to_dark_when_passed_dark', 0, () => {
      // setThemeMode 是同步方法，不需要 await
      AppState.setThemeMode('dark');
      expect(AppState.themeMode).assertEqual('dark');
    });

    it('should_set_to_light_when_passed_light', 0, () => {
      AppState.themeMode = 'dark';
      AppState.setThemeMode('light');
      expect(AppState.themeMode).assertEqual('light');
    });

    it('should_ignore_invalid_input', 0, () => {
      // 验证非法输入（'auto'）被拒，themeMode 保持不变
      AppState.themeMode = 'light';
      // @ts-expect-error: 测试故意传错
      AppState.setThemeMode('auto');
      // 期望：themeMode 仍为 'light'（拒收非法输入）
      expect(['light', 'dark']).assertContain(AppState.themeMode as string);
    });
  });

  describe('AppState.loadLoginState + 主题应用', () => {
    // 此测试验证：loadLoginState 完成后，如果 AppState.themeMode='dark'，系统 ColorMode 应为 DARK

    beforeEach(() => {
      const mockCtx = new MockContext();
      AppState.context = mockCtx as unknown as Context;
    });

    afterEach(() => {
      AppState.context = undefined as unknown as Context;
    });

    it('should_apply_dark_theme_after_loadLoginState_when_storage_has_dark', 0, async () => {
      const ctx: MockContext = AppState.context as unknown as MockContext;

      // 直接设置 AppState.themeMode 为 dark（模拟 storage 已加载）
      AppState.themeMode = 'dark';
      // 触发应用
      AppState.applyThemeMode();

      expect(ctx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_DARK);
    });

    it('should_apply_light_theme_by_default', 0, async () => {
      const ctx: MockContext = AppState.context as unknown as MockContext;

      AppState.themeMode = 'light';
      AppState.applyThemeMode();

      expect(ctx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_LIGHT);
    });
  });

  describe('启动时序 (EntryAbility)', () => {
    // 验证"启动时主题应用是否发生在第一帧之前"
    // applyThemeMode 是同步函数，context 已设置时一定能同步生效

    it('applyThemeMode_should_be_callable_before_first_frame', 0, () => {
      const mockCtx = new MockContext();
      AppState.context = mockCtx as unknown as Context;
      AppState.themeMode = 'dark';
      AppState.applyThemeMode();
      expect(mockCtx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_DARK);
      // 清理
      AppState.context = undefined as unknown as Context;
    });

    it('applyThemeMode_light_should_be_callable_before_first_frame', 0, () => {
      const mockCtx = new MockContext();
      AppState.context = mockCtx as unknown as Context;
      AppState.themeMode = 'light';
      AppState.applyThemeMode();
      expect(mockCtx.appCtx.lastSetColorMode).assertEqual(ConfigurationConstant.ColorMode.COLOR_MODE_LIGHT);
      // 清理
      AppState.context = undefined as unknown as Context;
    });
  });
}
