/**
 * 日期工具函数
 * @author 胡宪棋 软件2413 202421332084
 */
import dayjs from 'dayjs'

/** 格式化日期 yyyy-MM-dd HH:mm:ss */
export function formatDateTime(date) {
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

/** 格式化日期 yyyy-MM-dd */
export function formatDate(date) {
  return dayjs(date).format('YYYY-MM-DD')
}

/** 获取当前月份 yyyy-MM */
export function getCurrentMonth() {
  return dayjs().format('YYYY-MM')
}

/** 格式化金额 */
export function formatMoney(amount) {
  if (amount === null || amount === undefined) return '¥0.00'
  return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 获取近6个月的月份数组 */
export function getRecent6Months() {
  const months = []
  for (let i = 5; i >= 0; i--) {
    months.push(dayjs().subtract(i, 'month').format('YYYY-MM'))
  }
  return months
}
