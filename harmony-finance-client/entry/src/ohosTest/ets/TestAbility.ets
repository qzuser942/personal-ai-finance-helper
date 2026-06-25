/**
 * 单元测试入口
 * @ohos/hypium 测试运行器入口
 */
import { abilityDelegatorRegistry, TestRunner } from '@kit.TestKit';
import { logger } from '@kit.PerformanceAnalysisKit';
import { BusinessError } from '@kit.BasicServicesKit';
import DarkModeTest from './test/DarkMode.test.ets';

async function onAbilityCreateCallback() {
  console.info('onAbilityCreateCallback');
}

export default class TestAbility extends TestRunner {
  constructor() {
    super(['/test/ets/test/List.test.ets']);
  }

  onPrepare() {
    console.info('=== TestAbility onPrepare ===');
  }

  async onRun() {
    // 注册 dark mode 测试套件
    DarkModeTest();
    console.info('=== TestAbility onRun ===');
  }
}

abilityDelegatorRegistry.getAbilityDelegator()
  .then((delegator) => {
    delegator.addAbilityMonitor(
      {
        abilityName: 'TestAbility',
        onAbilityCreate: onAbilityCreateCallback
      },
      (err: BusinessError) => {
        if (err) {
          console.error(`addAbilityMonitor fail, err: ${JSON.stringify(err)}`);
        }
      }
    );
    logger.info('start run testcase');
  })
  .catch((err: BusinessError) => {
    logger.error(`getAbilityDelegator fail, err: ${JSON.stringify(err)}`);
  });
