/**
 * Excel导出工具
 * @author 胡宪棋 软件2413 202421332084
 */
import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'

/**
 * 将数据导出为Excel文件
 * @param {Array} data - 数据数组
 * @param {string} filename - 文件名（不含扩展名）
 * @param {Array} headers - 表头 [{key, title}]
 */
export function exportToExcel(data, filename, headers) {
  try {
    const sheetData = data.map(row => {
      const obj = {}
      headers.forEach(h => {
        obj[h.title] = row[h.key] !== undefined ? row[h.key] : ''
      })
      return obj
    })

    const ws = XLSX.utils.json_to_sheet(sheetData)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')

    ws['!cols'] = headers.map(() => ({ wch: 18 }))

    XLSX.writeFile(wb, `${filename}_${Date.now()}.xlsx`)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}
