/**
 * 文件操作工具类
 * 解决 mkdirSync 跨 API 版本兼容性问题，防止 "No such file or directory"
 * @author 胡宪棋 软件2413 202421332084
 */
import fs from "@ohos.file.fs";

const TAG = "FileUtil";

export class FileUtil {
  /**
   * 确保目录存在（同步，兼容 API 9/10/12+）
   *
   * 为什么不用 fs.mkdirSync(path, true) 的 recursive 参数？
   *   - API 9:  mkdirSync(path, mode)  第二个参数是 mode（数字），true→1 不会递归
   *   - API 10+: mkdirSync(path, recursion) 第二个参数才是递归标志
   * 因此手动递归创建，确保跨版本兼容。
   *
   * 为什么不用逐级从根目录创建？
   *   - HarmonyOS 沙箱限制 App 访问系统级目录（如 /data、/data/storage）
   *   - fs.accessSync("/data") 会抛异常，导致误判并尝试 mkdirSync("/data") 失败
   * 改用自底向上递归：先尝试直接创建目标目录，失败则递归创建父目录后再试。
   *
   * @param dirPath 要确保存在的目录绝对路径
   */
  static ensureDirSync(dirPath: string): void {
    try {
      fs.accessSync(dirPath);
      return;
    } catch (e) {
      // 目录不存在，需创建
    }

    try {
      fs.mkdirSync(dirPath);
      console.info(`${TAG}: 已创建目录 ${dirPath}`);
      return;
    } catch (e) {
      // mkdirSync 失败，可能是父目录不存在
    }

    const normalized = dirPath.replace(/\\/g, "/");
    const lastSep = normalized.lastIndexOf("/");
    if (lastSep <= 0) {
      throw new Error(`无法创建目录: ${dirPath}（根路径不可创建）`);
    }

    const parent = normalized.substring(0, lastSep);
    FileUtil.ensureDirSync(parent);

    try {
      fs.mkdirSync(dirPath);
      console.info(`${TAG}: 已创建目录 ${dirPath}`);
    } catch (mkdirErr) {
      console.error(`${TAG}: 创建目录 ${dirPath} 失败:`, JSON.stringify(mkdirErr));
      throw new Error(`无法创建目录: ${dirPath}`);
    }
  }

  /**
   * 获取应用沙箱内的导出目录路径，并确保其存在
   */
  static ensureExportDir(filesDir: string): string {
    const exportsDir = `${filesDir}/exports`;
    FileUtil.ensureDirSync(exportsDir);
    return exportsDir;
  }
}
