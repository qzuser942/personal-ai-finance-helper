/**
 * 存钱目标模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse } from '../request';
import { SaveTarget, SaveTargetCreateRequest, SaveTargetUpdateRequest } from '../../model/SaveTargetModel';

export class SaveTargetApi {
  /** 获取存钱目标列表 GET /api/save-target/list */
  static async getList(status?: number): Promise<ApiResponse<SaveTarget[]>> {
    return HttpRequest.get('/api/save-target/list', status !== undefined ? { status } : undefined);
  }

  /** 创建存钱目标 POST /api/save-target */
  static async create(data: SaveTargetCreateRequest): Promise<ApiResponse<SaveTarget>> {
    return HttpRequest.post('/api/save-target', data);
  }

  /** 更新/追加存款 PUT /api/save-target/{id} */
  static async update(id: number, data: SaveTargetUpdateRequest): Promise<ApiResponse<SaveTarget>> {
    return HttpRequest.put(`/api/save-target/${id}`, data);
  }

  /** 删除存钱目标 DELETE /api/save-target/{id} */
  static async delete(id: number): Promise<ApiResponse<null>> {
    return HttpRequest.delete(`/api/save-target/${id}`);
  }
}
