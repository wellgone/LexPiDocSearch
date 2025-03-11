import request from "@/utils/request";


export interface Report {
  id: number;
  title: string;
  userId: string;
  createTime: string;
  modifiedTime: string;
}

export async function getUserReports() {
  return request<{
    code: number;
    message: string;
    data: Report[];
  }>(`/api/report/list`, {
    method: 'GET',
  });
} 