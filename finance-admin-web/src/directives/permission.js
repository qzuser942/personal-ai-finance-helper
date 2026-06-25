/**
 * v-permission 自定义指令
 * <p>统一从后端 /api/admin/info 拉取的 permissions 权限位判断按钮是否渲染。
 * <p>避免 localStorage 篡改导致的越权误判。
 *
 * <p>使用示例：
 * <pre>
 *   &lt;el-button v-permission="'bill:write'"&gt;编辑账单&lt;/el-button&gt;
 *   &lt;el-button v-permission="['bill:write','bill:delete']"&gt;管理&lt;/el-button&gt;
 * </pre>
 *
 * <p>支持传单值或数组（数组时，任一权限位通过即显示）：
 * - 'bill:write' → 必须有 bill:write 才渲染
 * - ['bill:write','bill:delete'] → 有任一权限位即渲染
 *
 * <p>权限位异步加载完成后会重新评估元素可见性。
 *
 * @author 胡宪棋 软件2413 202421332084
 */
import { useUserStore } from '@/store/user'
import { watch } from 'vue'

/**
 * 检查权限位是否通过
 * @param {string|string[]} perm 权限位或权限位数组
 * @param {{isSuperAdmin: boolean, hasPermission: Function}} store 用户 store
 * @returns {boolean}
 */
function checkPermission(perm, store) {
  if (perm == null) return true
  if (Array.isArray(perm)) {
    return perm.some(p => store.hasPermission(p))
  }
  return store.hasPermission(perm)
}

/**
 * 关键修复：使用 v-show 风格的 display 控制（更稳定）
 * <p>无权限时把元素从 DOM 中移除（不渲染），避免被 hack 触发
 * <p>权限位变化时立即重新评估
 */
function apply(el, perm, userStore) {
  if (!checkPermission(perm, userStore)) {
    el.style.display = 'none'
    el.setAttribute('data-permission-hidden', '1')
  } else {
    el.style.display = ''
    el.removeAttribute('data-permission-hidden')
  }
}

export default {
  install(app) {
    /**
     * v-permission
     */
    app.directive('permission', {
      mounted(el, binding) {
        const userStore = useUserStore()
        apply(el, binding.value, userStore)
        // 关键修复：监听权限位变化，异步加载完成后重新评估
        const stop = watch(
          () => [userStore.permissions, userStore.role],
          () => apply(el, binding.value, userStore),
          { deep: true }
        )
        el.__permissionStop = stop
      },
      updated(el, binding) {
        const userStore = useUserStore()
        apply(el, binding.value, userStore)
      },
      unmounted(el) {
        if (el.__permissionStop) {
          el.__permissionStop()
          delete el.__permissionStop
        }
      }
    })

    /**
     * v-permission-disabled
     * <p>无权限时元素可见但禁用点击
     */
    app.directive('permission-disabled', {
      mounted(el, binding) {
        const userStore = useUserStore()
        const update = () => {
          if (!checkPermission(binding.value, userStore)) {
            el.disabled = true
            el.classList.add('is-disabled-no-perm')
            el.title = '权限不足'
          } else {
            el.disabled = false
            el.classList.remove('is-disabled-no-perm')
            el.removeAttribute('title')
          }
        }
        update()
        const stop = watch(
          () => [userStore.permissions, userStore.role],
          update,
          { deep: true }
        )
        el.__permissionDisabledStop = stop
      },
      unmounted(el) {
        if (el.__permissionDisabledStop) {
          el.__permissionDisabledStop()
          delete el.__permissionDisabledStop
        }
      }
    })
  }
}
