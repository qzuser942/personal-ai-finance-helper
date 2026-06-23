/**
 * 分类模块API
 * @author 胡宪棋 软件2413 202421332084
 */
import { HttpRequest, ApiResponse } from '../request';
import { Category, CategoryCreateRequest } from '../../model/CategoryModel';

export class CategoryApi {
  /** 获取分类列表 GET /api/category/list */
  static async getList(type?: string): Promise<ApiResponse<Category[]>> {
    return HttpRequest.get('/api/category/list', type ? { type } : undefined);
  }

  /** 创建自定义分类 POST /api/category */
  static async create(data: CategoryCreateRequest): Promise<ApiResponse<Category>> {
    return HttpRequest.post('/api/category', data);
  }

  /** 修改自定义分类 PUT /api/category/{id} */
  static async update(id: number, data: Partial<CategoryCreateRequest>): Promise<ApiResponse<Category>> {
    return HttpRequest.put(`/api/category/${id}`, data);
  }

  /** 删除自定义分类 DELETE /api/category/{id} */
  static async delete(id: number): Promise<ApiResponse<null>> {
    return HttpRequest.delete(`/api/category/${id}`);
  }
}
