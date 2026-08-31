export type TUserRole = "BUYER" | "SELLER" | "ADMIN";

export type TAuthUser = {
  id: string;
  email: string;
  name: string;
  roles: TUserRole[];
};

export type TLoginRequest = {
  email: string;
  password: string;
};

export type TLoginResponse = {
  accessToken: string;
  idToken?: string;
  refreshToken?: string;
  expiresIn: number;
  tokenType: string;
  user: {
    id: string;
    email: string;
    roles: string[];
  };
};
