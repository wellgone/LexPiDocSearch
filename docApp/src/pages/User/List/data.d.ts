export interface UserType {
  userId: string;
  userAccount: string;
  userName: string;
  userEmail: string;
  userRole: 'admin' | 'user';
  userState: number;
  userLoginNum: number;
  createTime: string;
  modifiedTime: string;
}

export interface UserFormValues {
  userAccount: string;
  userPassword?: string;
  userName: string;
  userEmail: string;
  userRole: 'admin' | 'user';
  userState: number;
} 