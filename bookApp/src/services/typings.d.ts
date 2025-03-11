declare namespace API {
  interface Response<T = any> {
    code: number;
    message?: string;
    data?: T;
  }
} 