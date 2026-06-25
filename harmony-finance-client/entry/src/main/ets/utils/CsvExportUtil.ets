/**
 * 本地CSV导出工具（Plan B：后端Excel导出失败时的替代方案）
 * 直接从API查询账单数据，在本地生成CSV文件保存到应用沙箱
 * @author 胡宪棋 软件2413 202421332084
 */
import fs from "@ohos.file.fs";
import environment from "@ohos.file.environment";
import common from "@ohos.app.ability.common";
import { AppState } from "../store/AppState";
import { BillApi } from "../network/api/billApi";
import { BillListItem } from "../model/BillModel";
import { FileUtil } from "./FileUtil";

const TAG = "CsvExportUtil";

export class CsvExportUtil {
  /**
   * 导出账单为CSV文件到本地
   * @param yearMonth 年月筛选（如 "2026-06"），可选
   * @param type 收支类型筛选（"income"/"expense"），可选
   * @returns 文件保存路径
   */
  static async exportToCsv(yearMonth?: string, type?: string): Promise<string> {
    const ctx: common.Context = AppState.context;
    if (!ctx) {
      throw new Error("AppState.context 未初始化");
    }

    const bills: BillListItem[] = await CsvExportUtil.fetchAllBills(yearMonth, type);
    if (bills.length === 0) {
      throw new Error("没有可导出的账单数据");
    }

    const csvContent: string = CsvExportUtil.buildCsv(bills);
    const timestamp: string = CsvExportUtil.formatTimestamp(new Date());
    const fileName: string = `bills_${yearMonth || "all"}_${timestamp}.csv`;

    const savePath = CsvExportUtil.writeTextToFile(ctx, fileName, csvContent);
    if (!savePath) {
      throw new Error("CSV文件写入失败");
    }

    try {
      const publicDir = environment.getUserDownloadDir();
      FileUtil.ensureDirSync(publicDir);
      const publicPath = `${publicDir}/${fileName}`;
      fs.copyFileSync(savePath, publicPath);
      return publicPath;
    } catch (e) {
      console.info(`${TAG}: 复制到公共下载目录失败，使用沙箱路径`, JSON.stringify(e));
      return savePath;
    }
  }

  /**
   * 将文本内容写入文件（多级回退策略）
   */
  private static writeTextToFile(
    ctx: common.Context,
    fileName: string,
    content: string,
  ): string | null {
    const candidates: string[] = [`${ctx.filesDir}/exports`, ctx.filesDir];
    if (ctx.cacheDir) {
      candidates.push(ctx.cacheDir);
    }

    for (const dir of candidates) {
      try {
        FileUtil.ensureDirSync(dir);
        const filePath = `${dir}/${fileName}`;

        const fd = fs.openSync(
          filePath,
          fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY | fs.OpenMode.TRUNC,
        );
        try {
          fs.writeSync(fd.fd, content);
          console.info(`${TAG}: CSV写入成功: ${filePath}`);
          return filePath;
        } finally {
          fs.closeSync(fd);
        }
      } catch (e) {
        console.warn(`${TAG}: 写入 ${dir} 失败:`, JSON.stringify(e));
      }
    }

    console.error(`${TAG}: 所有路径写入均失败`);
    return null;
  }

  /**
   * 分页拉取全部账单数据
   */
  private static async fetchAllBills(yearMonth?: string, type?: string): Promise<BillListItem[]> {
    const allBills: BillListItem[] = [];
    const pageSize: number = 100;
    let page: number = 1;
    let hasMore: boolean = true;

    while (hasMore) {
      const res = await BillApi.search({
        page: page,
        size: pageSize,
        yearMonth: yearMonth,
        type: type,
      });

      if (res.code === 200 && res.data) {
        const records = res.data.records;
        allBills.push(...records);
        hasMore = page < res.data.totalPages;
        page++;
      } else {
        hasMore = false;
      }
    }

    return allBills;
  }

  /**
   * 构建CSV内容（BOM + 表头 + 数据行）
   */
  private static buildCsv(bills: BillListItem[]): string {
    const BOM: string = "\uFEFF";
    const header: string = "序号,日期,类型,分类,金额,备注\n";
    const rows: string[] = bills.map((b, index) => {
      const typeLabel: string = b.type === "income" ? "收入" : "支出";
      const amount: string = (b.amount ?? 0).toFixed(2);
      const remark: string = CsvExportUtil.escapeCsvField(b.remark || "");
      const categoryName: string = CsvExportUtil.escapeCsvField(b.categoryName || "");
      const consumeTime: string = b.consumeTime || "";
      return `${index + 1},${consumeTime},${typeLabel},${categoryName},${amount},${remark}`;
    });

    return BOM + header + rows.join("\n");
  }

  /**
   * 转义CSV字段（含逗号或引号时用双引号包裹）
   */
  private static escapeCsvField(field: string): string {
    if (field.includes(",") || field.includes('"') || field.includes("\n")) {
      return `"${field.replace(/"/g, '""')}"`;
    }
    return field;
  }

  /**
   * 格式化时间戳为文件名安全格式
   */
  private static formatTimestamp(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const h = String(date.getHours()).padStart(2, "0");
    const min = String(date.getMinutes()).padStart(2, "0");
    const s = String(date.getSeconds()).padStart(2, "0");
    return `${y}${m}${d}_${h}${min}${s}`;
  }
}
